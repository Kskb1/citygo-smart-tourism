# 设计说明

## 架构

- `frontend/`：Vue 3、Vite、Element Plus、Axios、Vue Router。
- `backend/`：Spring Boot 3、WebClient、Spring Validation、MyBatis Plus 预留、MySQL。
- `database/`：MySQL 8 建表和真实辅助数据初始化脚本。

## Provider 分层

后端把外部数据接入封装为 Provider：

- `AmapPoiClient`：高德 POI / 景点关键词搜索。
- `AmapWeatherClient`：高德天气查询。
- `AmapRouteClient`：高德驾车、步行、公交路线规划。
- `AmapGeocodeClient`：地址 / 城市经纬度解析。
- `AmapProvider`：聚合上述客户端，给控制器和规划服务提供统一入口。
- `AmadeusAuthClient`：OAuth2 token。
- `AmadeusFlightClient`：航班报价。
- `AmadeusHotelClient`：酒店列表与报价。
- `HotelPriceService`：酒店报价服务入口，只透传真实 API 报价，不补价格。
- `Official12306RedirectProvider`：12306 官方查询跳转。
- `ThirdPartyTrainApiProvider`：合规第三方高铁接口预留，未接入供应商时不返回价格。
- `TrainProvider`：按配置选择 12306 官方跳转或合规第三方接口预留。

统一返回结构：

```json
{
  "sourceName": "Amap Open Platform",
  "fetchedAt": "2026-06-16T15:30:00+08:00",
  "realData": true,
  "rawJson": {},
  "errorMessage": null
}
```

## 前端页面

- 首页：数据源状态。
- 智能规划：聚合真实接口结果。
- 交通查询：航班真实报价与高铁官方跳转。
- 酒店查询：真实酒店报价。
- 景点推荐：高德 POI。
- 后台管理：Provider 状态、API 日志、缓存入口。
