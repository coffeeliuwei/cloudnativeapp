#!/bin/bash

echo "===== 检查 A. 包名一致性 ====="
for file in $(find . -name "*.java" -type f ! -path "*/.mvn/*"); do
    # 获取包名
    package=$(grep "^package" "$file" | sed 's/package //;s/;$//' | tr '.' '/')
    # 获取相对路径（去掉src/main/java和文件名）
    relpath=$(echo "$file" | sed 's|^\./||;s|/src/main/java/||;s|/[^/]*\.java$||')
    
    if [ "$package" != "$relpath" ]; then
        echo "❌ MISMATCH: $file"
        echo "   Expected package: $relpath"
        echo "   Actual package:   $package"
    fi
done
echo "✅ 包名一致性检查完成"
