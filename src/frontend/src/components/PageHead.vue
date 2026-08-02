<script setup lang="ts">defineProps<{ eyebrow:string; title:string; description:string }>()</script>
<template><header class="page-head"><div><p>{{ eyebrow }}</p><h1>{{ title }}</h1><span>{{ description }}</span></div><div class="page-actions"><slot /></div></header></template>
