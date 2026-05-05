<style>
    @import './order.less';
</style>
<template>
    <div class="order-page">
        <!-- 查询条件 -->
        <Row>
            <Form ref="searchForm" :model="searchForm" inline class="search-form">
                <FormItem>
                    <Input type="text" v-model="searchForm.order_id" clearable placeholder="输入订单号"
                    style="width: 80px"/>
                </FormItem>

                <FormItem>
                    <Button type="primary" @click="handleSubmit">搜索</Button>
                </FormItem>
            </Form>
        </Row>
        <Row>
            <Table stripe border :columns="columns" :data="data" @on-row-click='getTrip'></Table>
            <!-- 订单列表分页 -->
            <Row type="flex" justify="end" class="page">
                <Page :current="searchForm.pageNum" :total="total" :page-size="searchForm.pageSize"
                page-size-opts="[10,20,50]" size="small" show-total show-elevator show-sizer></Page>
            </Row>
        </Row>
    </div>
</template>
<script>
import vm from './order.js'
export default vm
</script>
