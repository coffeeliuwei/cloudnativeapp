<style lang="less">
  @import './login.less';
</style>

<template>
  <div class="coffee-login">
    <div class="coffee-login__card">
      <div class="coffee-login__logo">
        <div class="coffee-login__icon-wrap">☕</div>
        <span class="coffee-login__brand">COFFEETRACK</span>
      </div>
      <div class="coffee-login__form">
        <login-form @on-success-valid="handleSubmit"></login-form>
      </div>
    </div>
  </div>
</template>

<script>
import LoginForm from '_c/login-form'
import { mapActions } from 'vuex'
export default {
  components: {
    LoginForm
  },
  methods: {
    ...mapActions([
      'handleLogin',
      'getUserInfo'
    ]),
    handleSubmit ({ userName, password }) {
      this.handleLogin({ userName, password }).then(res => {
        this.getUserInfo().then(res => {
          this.$router.push({
            name: this.$config.homeName
          })
        })
      })
    }
  }
}
</script>
