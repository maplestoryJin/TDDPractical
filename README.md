# RESTful Web Services with TDD  [![Java CI with Gradle](https://github.com/maplestoryJin/TDDPractical/actions/workflows/gradle.yml/badge.svg)](https://github.com/maplestoryJin/TDDPractical/actions/workflows/gradle.yml)

## 任务列表
* ~~ResourceServlet~~
  * ~~将请求派分给对应的资源（Resource），并根据返回的状态、超媒体类型、内容，响应 Http 请求~~
    * ~~使用 OutboundResponse 的 status 作为 Http Response 的状态；~~
    * ~~使用 OutboundResponse 的 headers 作为 Http Response 的 Http Headers；~~
    * ~~通过 MessageBodyWriter 将 OutboundResponse 的 GenericEntity 写回为 Body；~~
    * ~~如果找不到对应的 MessageBodyWriter，则返回 500 族错误~~
    * ~~如果找不到对应的 HeaderDelegate，则返回 500 族错误~~
    * ~~如果找不到对应的 ExceptionMapper，则返回 500 族错误~~
    * ~~如果 entity 为空， 则忽略body~~
  * ~~当资源方法抛出异常时，根据异常影响 Http 请求~~
    * ~~如果抛出 WebApplicationException，且 response 不为 null，则使用 response 响应 Http~~
    * ~~如果抛出的不是 WebApplicationException，则通过异常的具体类型查找 ExceptionMapper，生产 response 响应 Http 请求~~
  * ~~当其他组件抛出异常时，根据异常响应 Http 请求~~
    * ~~调用 ExceptionMapper 时~~
    * ~~调用 HeaderDelegate 时~~
    * ~~调用 MessageBodyWriter 时~~
    * ~~通过 Providers 查找 ExceptionMapper 时~~
    * ~~通过 RuntimeDelegate 查找 HeaderDelegate 时~~
* ~~ResourceRouter~~
  * ~~将 Resource Method 的返回值包装为 Response 对象~~
    * ~~根据与Path匹配结果，降序排列RootResource，选择第一个的RootResource~~
    * ~~如果没有匹配的 RootResource，则构造 404 的 Response~~
    * ~~如果返回的 RootResource 中无法匹配剩余Path， 则构造 404 的 Response~~
    * ~~如果 ResourceMethod 返回 null，则构造 204 的 Response~~
* Resource/RootResource/ResourceMethod
  * 在处理请求派分时，可以支持多级子资源（Sub-Resource）
    * ~~当没有资源方法可以匹配请求时，选择最优匹配 SubResourceLocator，通过它继续进行派分~~
    * 如果 SubResourceLocator 也无法找到满足的请求时，返回 404
  * 在处理请求派分时，可以根据客户端提供的超媒体类型，选择对应的资源方法（Resource Method）
  * ~~在处理请求派分时，可以根据客户端提供的 Http 方法，选择对应的资源方法~~
    * ~~当请求与资源方法的 Uri 模版一致，且 Http 方法一致时，派分到该方法~~
    * ~~没有资源方法于请求的 Uri 和 Http 方法一致时，返回 404~~
  * 资源方法可以返回 Java 对象，由 Runtime 自行推断正确的返回状态
  * 资源方法可以不明确指定返回的超媒体类型，由 Runtime 自行推断，比如，资源方法标注了 Produces，那么就使用标注提供的超媒体类型等
  * 资源方法可按找期望的类型，访问 Http 请求的内容
  * 资源对象和资源方法可接受环境组件的注入
  * ~~从 Path 标注中获取 UriTemplate~~
    * 如不存在 Path 标注，则抛出异常
* Providers
  * 可通过扩展点 MessageBodyWriter 处理不同类型的返回内容
  * 可通过扩展点 ExceptionMapper 处理不同类型的异常
* ResourceContext
  * 资源对象和资源方法可接受环境组件的注入
* RuntimeDelegate
  * 为 MediaType 提供 HeaderDelegate
  * 为 CacheControl 提供 HeaderDelegate
  * 为 Cookie 提供 HeaderDelegates
  * 为 EntityTag 提供 HeaderDelegate
  * 为 Link 提供 HeaderDelegate
  * 为 NewCookie 提供 HeaderDelegate
  * 为 Date 提供 HeaderDelegate
  * 提供 OutboundResponseBuilder
* OutboundResponseBuilder
* OutboundResponse
* ~~UriTemplate~~
  * ~~匹配无参数的 Uri 模版~~
    * ~~如果 Uri 可以与模版匹配，则返回匹配结果~~
    * ~~如果 Uri 不能与模版匹配，则返回 Optional.empty~~
  * ~~匹配带参数的 Uri 模版~~
    * ~~如果 Uri 可以与模版匹配，按照指定参数从 Uri 中提取值~~
    * ~~参数可以通过正则表达式指定格式~~
    * ~~如果参数重复定义，则抛出异常~~
  * ~~模版匹配的结果可以比较大小~~
    * ~~如果匹配的非参字符多，则优先（长的优先）~~
    * ~~如果匹配的非参数字符一样，匹配的分组多，则优先（参数优先）~~
    * ~~如果匹配的分组一样多，指定格式参数匹配多的优先（指定格式参数优先）~~