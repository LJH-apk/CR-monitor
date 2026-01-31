# ============ Nginx服务 ============
  FROM nginx:1.25-alpine

  # 安装curl用于健康检查
  RUN apk add --no-cache curl

  # 复制本地构建产物
  COPY dist /usr/share/nginx/html

  # 复制Nginx配置
  COPY nginx.conf /etc/nginx/conf.d/default.conf

  # 暴露端口
  EXPOSE 80

  # 健康检查
  HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
      CMD curl -f http://localhost/ || exit 1

  CMD ["nginx", "-g", "daemon off;"]