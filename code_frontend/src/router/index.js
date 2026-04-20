import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Jobs from '../views/Jobs.vue'
import Learn from '../views/Learn.vue'
import Algo from '../views/ListView.vue'
import Match from '../views/Match.vue'
import Interview from '../views/Interview.vue'
import Profile from '../views/Profile.vue'
import Detail from '../views/DetailView.vue'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            name: 'Home',
            component: Home
        },
        {
            path: '/jobs',
            name: 'Jobs',
            component: Jobs
        },
        {
            path: '/learn',
            name: 'Learn',
            component: Learn
        },
        {
            path: '/algo',
            name: 'Algo',
            component: Algo
        },
        {
            path: '/match',
            name: 'Match',
            component: Match
        },
        {
            path: '/interview',
            name: 'Interview',
            component: Interview
        },
        {
            path: '/profile',
            name: 'Profile',
            component: Profile
        },
        {
            path: '/detail/:id',
            name: 'Detail',
            component: Detail,
            props: true
        }
    ]
})

export default router