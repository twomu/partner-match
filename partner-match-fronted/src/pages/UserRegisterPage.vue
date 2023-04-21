<template>
  <van-form @submit="onSubmit">
    <van-cell-group inset>
      <van-field
          v-model="userAccount"
          name="userAccount"
          label="账号"
          placeholder="请输入账号"
          :rules="[{ required: true, message: '请填写用户名' }]"
      />
      <van-field
          v-model="userPassword"
          type="password"
          name="userPassword"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请填写密码' }]"
      />
      <van-field
          v-model="checkPassword"
          type="password"
          name="checkPassword"
          label="确认密码"
          placeholder="重新输入密码"
          :rules="[{ required: true, message: '重新输入密码' }]"
      />
      <van-field
          v-model="userCode"
          type="text"
          name="planetCode"
          label="用户编号"
          placeholder="请输入用户编号"
          :rules="[{ required: true, message: '请输入用户编号' }]"
      />
    </van-cell-group>
    <div style="margin: 16px;">
      <van-button round block type="primary" native-type="submit">
        注册
      </van-button>
    </div>
  </van-form>
  <van-notify v-model:show="show" type="warning">
    <van-icon name="bell" style="margin-right: 4px;" />
    <span>{{ msg}}</span>
  </van-notify>

</template>

<script setup lang="ts">

import {ref} from "vue";
import myAxios from "../plugins/myAxios";
import {Toast} from "vant";

const msg=ref('');
const show=ref(false);
const userAccount = ref('');
const userPassword = ref('');
const checkPassword = ref('');
const userCode = ref('');

const onSubmit = async () => {
  if(userPassword.value!==checkPassword.value){
    Toast.fail('两次密码不一致');
    return;
  }
  if(userAccount.value.length<4 ||userPassword.value.length<8){
    show.value=true;
    msg.value='帐号要大于4位，密码要大于8位',
    Toast.fail('注册失败');
    return;
  }
  const res = await myAxios.post('/user/register', {
    userAccount: userAccount.value,
    userPassword: userPassword.value,
    checkPassword: checkPassword.value,
    planetCode : userCode.value
  })
  console.log(res, '用户注册');
  if (res.code === 0 && res.data) {
    Toast.success('注册成功');
    // 跳转到之前的页面
    window.location.href = '/user/login';
  } else {
    Toast.fail('登录失败');
  }
};

</script>

<style scoped>

</style>
