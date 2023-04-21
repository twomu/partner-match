import {createApp} from 'vue'
import App from './App.vue'
import * as VueRouter from 'vue-router';
import routes from "./config/route";
import Vant, {Lazyload} from 'vant';
import 'vant/lib/index.css';
import '../global.css'

const app = createApp(App);
app.use(Vant);

const router = VueRouter.createRouter({
    // 内部提供了 history 模式的实现。为了简单起见，我们在这里使用 hash 模式。
    history: VueRouter.createWebHistory(),
    routes, // `routes: routes` 的缩写
})
app.use(router);
// 注册时设置`lazyComponent`选项
app.use(Lazyload, {
    lazyComponent: true,
});

app.mount('#app')
