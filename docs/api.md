# API 说明

## 系统配置

- `GET /api/system/provider-status`
- `GET /api/admin/provider-status`

返回高德、Amadeus、高铁供应商配置状态。

真实数据接口统一返回字段包含：

```json
{
  "provider": "amap",
  "message": "AMAP_API_KEY 未配置，无法获取真实地图/天气/景点数据。",
  "data": null,
  "sourceName": "Amap Open Platform",
  "fetchedAt": "2026-06-16T15:30:00+08:00",
  "realData": false,
  "rawJson": null,
  "errorMessage": "AMAP_API_KEY 未配置，无法获取真实地图/天气/景点数据。"
}
```

## 景点

`GET /api/spots/search?city=成都&keyword=景点`

调用高德 POI 搜索。未配置 `AMAP_API_KEY` 时返回 `realData=false`。

## 天气

`GET /api/weather?city=成都`

调用高德天气。未配置 Key 时不返回假天气。

## 路线

`GET /api/routes?origin=104.06,30.67&destination=104.08,30.66&mode=driving`

调用高德路线规划。`mode` 支持 `driving`、`walking`、`transit`，公交模式可补充 `city` 参数。参数应使用经纬度或高德支持的位置格式。

## 航班

`GET /api/flights/search?fromCity=武汉&toCity=成都&date=2026-07-10`

调用 Amadeus Flight Offers Search。未配置 Amadeus 时返回明确提示。

## 酒店

`GET /api/hotels/search?city=成都&checkIn=2026-07-10&checkOut=2026-07-13`

调用 Amadeus 酒店列表与报价接口。接口未返回报价时不补价格。

## 高铁

`GET /api/trains/search?fromCity=武汉&toCity=成都&date=2026-07-10`

默认返回 12306 官方跳转提示，不显示伪造价格。

## 智能规划

`POST /api/plans/generate`

请求体：

```json
{
  "fromCity": "武汉",
  "toCity": "成都",
  "departureDate": "2026-07-10",
  "days": 3,
  "budget": 3000,
  "people": 1,
  "interests": ["景点"],
  "trafficPreference": "flight",
  "hotelPreference": "standard"
}
```

返回 POI、天气、路线、航班、酒店的统一真实数据结果、预算估算、高铁官方跳转或合规第三方结果。预算中的航班和酒店费用只来自 Amadeus 返回的真实报价；未返回价格时为空并给出提示。

## 后台

- `GET /api/admin/api-logs`
- `GET /api/admin/dashboard`
- `GET /api/admin/cache/spots`
- `GET /api/admin/cache/hotels`
- `GET /api/admin/cache/flights`
- `GET /api/admin/cache/weather`
- `GET /api/admin/cache/routes`
