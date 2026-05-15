<style>
@import '../order/order.less';
</style>
<template>
  <div class="order-page">
    <!-- 查询条件 -->
    <Row style="margin-bottom: 12px;">
      <Form ref="searchForm" :model="searchForm" inline class="search-form">
        <FormItem>
          <Input type="text" v-model="searchForm.order_id" clearable placeholder="订单号"
            style="width: 120px"/>
        </FormItem>
        <FormItem>
          <Input type="text" v-model="searchForm.member_name" clearable placeholder="会员姓名"
            style="width: 100px"/>
        </FormItem>
        <FormItem>
          <Button type="primary" @click="handleSubmit">搜索</Button>
        </FormItem>
        <FormItem>
          <Button type="success" icon="md-add" @click="openCreate">新建订单</Button>
        </FormItem>
      </Form>
    </Row>

    <!-- 订单表格 -->
    <Row>
      <Table stripe border :columns="columns" :data="data"></Table>
      <Row type="flex" justify="end" class="page">
        <Page :current="searchForm.pageNum" :total="total" :page-size="searchForm.pageSize"
          size="small" show-total show-elevator></Page>
      </Row>
    </Row>

    <!-- 新建订单弹窗 -->
    <Modal v-model="showCreate" title="新建订单" :footer-hide="true" width="400">
      <Form label-position="left" :label-width="80">
        <FormItem label="订单编号">
          <Input v-model="createForm.order_id" placeholder="如 ORDER100"/>
        </FormItem>
        <FormItem label="会员 ID">
          <Input v-model="createForm.OneID" placeholder="如 U001"/>
        </FormItem>
        <FormItem label="金额（元）">
          <Input v-model="createForm.order_amount" placeholder="如 199"/>
        </FormItem>
        <FormItem v-if="createError">
          <span style="color: #ed4014;">{{ createError }}</span>
        </FormItem>
        <FormItem>
          <Button type="primary" :loading="creating" @click="handleCreate">确认创建</Button>
          <Button style="margin-left: 8px;" @click="showCreate = false">取消</Button>
        </FormItem>
      </Form>
    </Modal>
  </div>
</template>
<script>
import vm from './order-manage.js'
export default vm
</script>
