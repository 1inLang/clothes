<script setup lang="ts">
import { computed } from 'vue'
const props=defineProps<{value:string}>()
const tone=computed(()=>['已完成','正常','通过'].includes(props.value)?'green':['待审核','验收中','设计中'].includes(props.value)?'blue':['退回修改','停用'].some(v=>props.value.includes(v))?'red':['进行中','已立项'].includes(props.value)?'amber':'gray')
</script>
<template><span class="pill" :class="tone"><i/>{{ value }}</span></template>
