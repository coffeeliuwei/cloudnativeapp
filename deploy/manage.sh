#!/bin/bash
# =============================================================
# Coffee 微服务一键管理脚本
# 用法：
#   ./manage.sh start    启动本机部署的服务
#   ./manage.sh stop     停止本机部署的服务
#   ./manage.sh restart  重启本机部署的服务
#   ./manage.sh status   查看运行状态
#   ./manage.sh logs     实时查看日志（Ctrl+C 退出）
#
# 多 ECS 部署模式：
#   本脚本支持 "一台 ECS 跑一个微服务" 的标准微服务架构。
#   每台 ECS 的 jars/ 目录只放本机要跑的那一个 jar，脚本会
#   自动检测 jars/ 下存在的 jar，只管理对应的服务，跳过其他。
#   3 台 ECS 用完全相同的这一份脚本，无需修改。
# =============================================================

# ┌─────────────────────────────────────────────────────────┐
# │              ★ 学生必填配置区（唯一要改的一处）★         │
# │  替换为你自己的 MSE Nacos【内网】地址（结尾带 :8848）    │
# │  ECS 与 MSE 同 VPC，走内网更快/免费/安全；公网地址是给   │
# │  你本地电脑调试用的，ECS 上不要填公网地址。              │
# └─────────────────────────────────────────────────────────┘
NACOS_ADDR="mse-xxxxxxxx-p.nacos-ans.mse.aliyuncs.com:8848"

# ─── 目录配置（通常不需要修改）────────────────────────────
BASE_DIR=$(cd "$(dirname "$0")" && pwd)
JARS_DIR="$BASE_DIR/jars"
LOGS_DIR="$BASE_DIR/logs"
CONFIG_DIR="$BASE_DIR/config"

# ─── 服务定义 ──────────────────────────────────────────────
# 格式：[服务名]="JAR文件名 HTTP端口"
declare -A SVC_JAR
SVC_JAR[userorder]="coffee-userorder-provider-1.0-SNAPSHOT.jar"
SVC_JAR[expresstrack]="coffee-expresstrack-provider-1.0-SNAPSHOT.jar"
SVC_JAR[app]="coffee-app-0.0.1-SNAPSHOT.jar"

declare -A SVC_PORT
SVC_PORT[userorder]=7001
SVC_PORT[expresstrack]=8001
SVC_PORT[app]=8005

# 启动顺序：先启动两个 Provider，最后启动 App（消费者）
START_ORDER=(userorder expresstrack app)

# =============================================================
# 内部函数
# =============================================================

_pid_file() { echo "$BASE_DIR/$1.pid"; }
_log_file()  { echo "$LOGS_DIR/$1.log"; }

_get_pid() {
    local f
    f=$(_pid_file "$1")
    [ -f "$f" ] && cat "$f"
}

_is_running() {
    local pid
    pid=$(_get_pid "$1")
    [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null
}

_start_one() {
    local name=$1
    local jar="$JARS_DIR/${SVC_JAR[$name]}"
    local port="${SVC_PORT[$name]}"
    local log
    log=$(_log_file "$name")
    local pid_file
    pid_file=$(_pid_file "$name")

    # 检查 JAR 是否存在
    # 多 ECS 部署时本机通常只有一个 jar，未部署到本机的服务直接跳过（不算错误）
    if [ ! -f "$jar" ]; then
        echo "  [-] $name: 跳过（本机未部署此服务）"
        return 0
    fi

    # 已在运行
    if _is_running "$name"; then
        echo "  [~] $name: 已运行 (PID=$(cat "$pid_file"), 端口=$port)"
        return 0
    fi

    # 构建启动参数
    local extra_args="--dubbo.registry.address=nacos://$NACOS_ADDR"

    # userorder / expresstrack 加载各自的外部 application-dev.yml
    if [ "$name" != "app" ]; then
        extra_args="$extra_args --spring.config.additional-location=file:$CONFIG_DIR/$name/"
    fi

    echo "  [→] $name: 正在启动（端口=$port）..."
    nohup java -jar "$jar" $extra_args > "$log" 2>&1 &
    local pid=$!
    echo "$pid" > "$pid_file"

    # 等待最多 30 秒确认端口监听
    local i=0
    while [ $i -lt 30 ]; do
        sleep 1
        if ss -tlnp 2>/dev/null | grep -q ":$port "; then
            echo "  [✓] $name: 启动成功 (PID=$pid, 端口=$port)"
            return 0
        fi
        i=$((i+1))
    done

    echo "  [!] $name: 30 秒内未监听到端口 $port，请检查日志: $log"
    return 1
}

_stop_one() {
    local name=$1
    local pid_file
    pid_file=$(_pid_file "$name")

    if ! _is_running "$name"; then
        echo "  [~] $name: 未运行"
        rm -f "$pid_file"
        return
    fi

    local pid
    pid=$(cat "$pid_file")
    kill "$pid" 2>/dev/null
    local i=0
    while kill -0 "$pid" 2>/dev/null && [ $i -lt 15 ]; do
        sleep 1; i=$((i+1))
    done
    kill -9 "$pid" 2>/dev/null
    rm -f "$pid_file"
    echo "  [✓] $name: 已停止 (PID=$pid)"
}

_status_one() {
    local name=$1
    local port="${SVC_PORT[$name]}"
    local jar="$JARS_DIR/${SVC_JAR[$name]}"
    # 区分"未部署到本机"和"已部署但未运行"两种状态
    if [ ! -f "$jar" ]; then
        echo "  [-] $name: 未部署到本机"
    elif _is_running "$name"; then
        local pid
        pid=$(_get_pid "$name")
        echo "  [✓] $name: 运行中 (PID=$pid, 端口=$port)"
    else
        echo "  [✗] $name: 未运行（jar 存在但进程未启动）"
    fi
}

# =============================================================
# 主命令
# =============================================================

do_start() {
    echo ">>> 启动本机部署的服务（Nacos: $NACOS_ADDR）"
    mkdir -p "$LOGS_DIR"
    # 检查本机部署了哪些 jar，给学生一个明确的提示
    local found=0
    for name in "${START_ORDER[@]}"; do
        [ -f "$JARS_DIR/${SVC_JAR[$name]}" ] && found=$((found+1))
    done
    if [ $found -eq 0 ]; then
        echo "  [!] $JARS_DIR 下没有任何已知 jar，请确认是否上传成功"
        return 1
    fi
    echo "    本机检测到 $found 个 jar，开始按依赖顺序启动..."
    for name in "${START_ORDER[@]}"; do
        _start_one "$name"
    done
    echo ""
    do_status
}

do_stop() {
    echo ">>> 停止所有服务"
    for name in "${START_ORDER[@]}"; do
        _stop_one "$name"
    done
}

do_restart() {
    do_stop
    sleep 2
    do_start
}

do_status() {
    echo ">>> 服务状态"
    for name in "${START_ORDER[@]}"; do
        _status_one "$name"
    done
}

do_logs() {
    echo ">>> 实时日志（Ctrl+C 退出）"
    echo "--- userorder (7001) ---  --- expresstrack (8001) ---  --- app (8005) ---"
    tail -f "$LOGS_DIR/userorder.log" "$LOGS_DIR/expresstrack.log" "$LOGS_DIR/app.log" 2>/dev/null \
        || echo "日志文件不存在，请先执行 ./manage.sh start"
}

# =============================================================
# 入口
# =============================================================

case "$1" in
    start)   do_start   ;;
    stop)    do_stop    ;;
    restart) do_restart ;;
    status)  do_status  ;;
    logs)    do_logs    ;;
    *)
        echo "用法: $0 {start|stop|restart|status|logs}"
        echo ""
        echo "  start    启动所有服务"
        echo "  stop     停止所有服务"
        echo "  restart  重启所有服务"
        echo "  status   查看运行状态"
        echo "  logs     实时查看日志（Ctrl+C 退出）"
        exit 1
        ;;
esac
