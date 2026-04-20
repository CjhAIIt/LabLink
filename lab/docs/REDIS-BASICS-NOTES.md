# Redis 基础笔记

> 当前整理范围：基于现有截图内容，先整理 `String`、`Hash`、`List`、`Set`、`SortedSet` 的基础概念与常见命令。  
> 本文档暂不一次写全，后续可继续补充示例、应用场景、Java 操作代码、RedisTemplate 用法等内容。

## 1. Redis 中 key 的设计

Redis 没有像 MySQL 一样的 `Table` 概念，因此需要通过 `key` 的命名规则来区分不同业务、不同数据类型的数据。

### 1.1 为什么要设计 key

例如：

- 用户信息要存到 Redis
- 商品信息也要存到 Redis
- 如果用户 `id=1`，商品 `id=1`

这时如果直接使用 `1` 作为 key，就会发生冲突，因此需要给 key 增加业务前缀。

### 1.2 推荐的 key 结构

Redis 的 key 通常可以按层级组织，多个单词之间使用 `:` 分隔。

推荐格式：

```text
项目名:业务名:类型:id
```

这个格式不是固定规则，可以根据实际需要做删减或扩展。

例如项目名为 `heima` 时：

- 用户相关 key：`heima:user:1`
- 商品相关 key：`heima:product:1`

如果 value 是一个 Java 对象，可以先序列化为 JSON 字符串再存入 Redis。

示例：

| Key | Value |
| --- | --- |
| `heima:user:1` | `{"id":1,"name":"Jack","age":21}` |
| `heima:product:1` | `{"id":1,"name":"小米11","price":4999}` |

### 1.3 key 设计建议

- 使用统一前缀，便于区分业务
- 使用 `:` 作为层级分隔符，结构更清晰
- key 命名尽量见名知意
- 同一项目内保持命名风格统一

## 2. String 类型

String 是 Redis 中最基础、最常用的数据类型。

### 2.1 常见命令

| 命令 | 说明 |
| --- | --- |
| `SET key value` | 添加或修改一个 String 类型的键值对 |
| `GET key` | 根据 key 获取 String 类型的 value |
| `MSET key value [key value ...]` | 批量添加多个 String 类型的键值对 |
| `MGET key [key ...]` | 根据多个 key 批量获取多个 value |
| `INCR key` | 让一个整型 key 自增 1 |
| `INCRBY key increment` | 让一个整型 key 按指定步长自增 |
| `INCRBYFLOAT key increment` | 让一个浮点数类型的值按指定步长自增 |
| `SETNX key value` | 仅当 key 不存在时，才设置值 |
| `SETEX key seconds value` | 设置 String 值，并指定过期时间 |

### 2.2 适用场景

- 缓存普通字符串
- 缓存 JSON 对象
- 计数器
- 短期验证码、令牌等带过期时间的数据

### 2.3 补充说明

- 如果存储的是对象，实际开发中通常会转成 JSON 再保存
- 自增命令更适合操作数字类型的值

## 3. Hash 类型

Hash 适合存储对象结构的数据，可以理解为一个 `key` 对应多个 `field-value`。

### 3.1 常见命令

| 命令 | 说明 |
| --- | --- |
| `HSET key field value` | 添加或修改 hash 中指定 field 的值 |
| `HGET key field` | 获取 hash 中指定 field 的值 |
| `HMSET key field value [field value ...]` | 批量添加多个 field 的值 |
| `HMGET key field [field ...]` | 批量获取多个 field 的值 |
| `HGETALL key` | 获取一个 hash 中所有的 field 和 value |
| `HKEYS key` | 获取一个 hash 中所有 field |
| `HVALS key` | 获取一个 hash 中所有 value |
| `HINCRBY key field increment` | 让 hash 中某个字段按指定步长自增 |
| `HSETNX key field value` | 仅当 field 不存在时，才设置该 field 的值 |

### 3.2 适用场景

- 存储用户信息
- 存储商品信息
- 存储对象的多个属性

### 3.3 与 String 的简单对比

- `String` 更适合直接存整个字符串或整个 JSON
- `Hash` 更适合按字段拆分存储对象属性

## 4. List 类型

Redis 中的 List 类型与 Java 中的 `LinkedList` 类似，可以看作一个双向链表结构，既支持正向检索，也支持反向检索。

### 4.1 主要特征

- 有序
- 元素可以重复
- 插入和删除较快
- 查询速度一般

### 4.2 典型场景

- 消息队列的简单实现
- 按顺序存储数据
- 最新数据列表

### 4.3 待补充

- List 的常见命令
- 左插入 / 右插入的区别
- 实际业务示例

## 5. Set 类型

Redis 的 Set 结构与 Java 中的 `HashSet` 类似，可以理解为 value 为 `null` 的 `HashMap`。  
因为底层也是哈希结构，所以具备类似 `HashSet` 的特征。

### 5.1 主要特征

- 无序
- 元素不可重复
- 查找快
- 支持交集、并集、差集等操作

### 5.2 常见命令

| 命令 | 说明 |
| --- | --- |
| `SADD key member [member ...]` | 向 set 中添加一个或多个元素 |
| `SREM key member [member ...]` | 移除 set 中指定元素 |
| `SCARD key` | 返回 set 中元素个数 |
| `SISMEMBER key member` | 判断元素是否存在于 set 中 |
| `SMEMBERS key` | 获取 set 中所有元素 |
| `SINTER key1 key2 [key ...]` | 求多个 set 的交集 |
| `SDIFF key1 key2 [key ...]` | 求多个 set 的差集 |
| `SUNION key1 key2 [key ...]` | 求多个 set 的并集 |

### 5.3 典型场景

- 标签去重
- 共同关注、共同爱好
- 用户权限集合
- 抽奖名单去重

## 6. SortedSet 类型

Redis 的 SortedSet 是一个可排序的 Set 集合，与 Java 中的 `TreeSet` 有些类似。  
它的每个元素都会关联一个 `score`，可以基于 `score` 对元素排序。

底层实现可以理解为：

- 跳表（SkipList）
- Hash 表

### 6.1 主要特征

- 可排序
- 元素不重复
- 查询速度快

由于可排序，SortedSet 经常被用于实现排行榜等功能。

### 6.2 常见命令

| 命令 | 说明 |
| --- | --- |
| `ZADD key score member [score member ...]` | 向 sorted set 添加元素；如果元素已存在则更新 score |
| `ZREM key member [member ...]` | 删除 sorted set 中指定元素 |
| `ZSCORE key member` | 获取指定元素的 score 值 |
| `ZRANK key member` | 获取指定元素的排名 |
| `ZCARD key` | 获取 sorted set 中元素个数 |
| `ZCOUNT key min max` | 统计 score 在指定范围内的元素个数 |
| `ZINCRBY key increment member` | 让指定元素的 score 按步长递增 |
| `ZRANGE key start stop` | 按排名范围获取元素 |
| `ZRANGEBYSCORE key min max` | 按 score 范围获取元素 |
| `ZDIFF keynum [key ...]` | 求差集 |
| `ZINTER keynum [key ...]` | 求交集 |
| `ZUNION keynum [key ...]` | 求并集 |

### 6.3 典型场景

- 排行榜
- 热度排序
- 积分排名
- 带权重的有序集合统计

## 7. 五种常见数据类型对比

| 类型 | 是否有序 | 元素是否可重复 | 典型特点 | 常见场景 |
| --- | --- | --- | --- | --- |
| `String` | 否 | value 整体覆盖 | 最基础、最通用 | 普通缓存、计数器、JSON 字符串 |
| `Hash` | field 无序 | field 不可重复 | 适合存对象属性 | 用户信息、商品信息 |
| `List` | 是 | 可重复 | 适合顺序存储 | 消息队列、动态列表 |
| `Set` | 否 | 不可重复 | 查找快，支持集合运算 | 标签、去重、共同关系 |
| `SortedSet` | 是 | member 不可重复 | 可按 score 排序 | 排行榜、积分系统 |

## 8. 后续可继续补充的内容

后面可以继续往这份文档里补：

- 各数据类型的实际 Redis 命令示例
- Java / Spring Boot 中的操作方式
- `RedisTemplate` 常见 API
- 缓存穿透、缓存击穿、缓存雪崩
- Redis 过期策略与淘汰策略
- Bitmap、HyperLogLog、GEO、Stream 等高级结构
