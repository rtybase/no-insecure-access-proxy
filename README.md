# No insecure access proxy

This is a small project to protect people from accessing HTTP, and thus insecure or leaking privacy details, resources. It consists of

- a PAC file instructing the browser to redirect all HTTP requests to the configured proxy server and
- a Proxy server blocking access to anything hitting it, including HTTP requests and suggesting to the user to try HTTPS versions of the relevant resources.

Browsers must be configured with Automatic Proxy option with the link to the PAC file, more details below.

## Building

Build it from inside the simple-webserver folder

```
mvn clean verify
```

`target/` folder will be created with `simple-webserver-1.0.0-SNAPSHOT-dist.tar.gz` file in it. Unpack it

```
tar -xvzf simple-webserver-1.0.0-SNAPSHOT-dist.tar.gz
```

then

```
cd simple-server/
```

and finally (this script was tested with Linux and Mac OS only)

```
./run.sh
```

If you see this

```
cat logs/simple-webserver.log
INFO  [2019-03-08 12:17:43,050] com.rtybase.simpleserver.SimpleServer: Started file server on port '8181' with root folder 'etc/'.
INFO  [2019-03-08 12:17:43,053] com.rtybase.simpleserver.SimpleServer: Started proxy server on port '3000' with redirect URL folder 'http://127.0.0.1:8181/files/splash.html'.
```

and `nohup.out` is empty, then it's happily running.

## Configuration 

Modify `app.config` file, if needed

```
# this is the port for file sharing service, e.g. to get PAC file and splash HTML page
file-port=8181
# this is the port for the proxy, it always 303 to the splash page
proxy-port=3000
# local path to the "/files" folder with the content of the PAC file and splash scree
files-folder=etc/
# tells proxy where to redirect (303) the requests, in this case to the splash page
# hosted by the file sharing service
redirect-page=http://127.0.0.1:8181/files/splash.html
 
# for more details see https://www.javaworld.com/article/2074601/undocumented-oracle-jvm-httpserver-properties.html
# 1 second, time measurement interval
sun.net.httpserver.clockTick=1000
# 5 seconds, read timeout
sun.net.httpserver.maxReqTime=5
# 10 seconds, write timeout
sun.net.httpserver.maxRspTime=10
sun.net.httpserver.maxIdleConnections=50
```

## Content files

Two files come with the package

```
ls -a etc/files/
.       ..      proxy.pac   splash.html
```

Both can be amended without restarting the service. Check the content with

```
curl -vvv -X GET "http://127.0.0.1:8181/files/proxy.pac"
curl -vvv -X GET "http://127.0.0.1:8181/files/splash.html"
```

Check proxy in action with

```
curl -vvv --proxy 127.0.0.1:3000 -X GET "http://www.ggg.com/something.html"
```

it should return a 303 to the block page

```
HTTP/1.1 303 See Other
Connection: close
Date: Fri, 15 Mar 2019 12:52:41 GMT
Content-type: text/html
Proxy-connection: close
Content-length: 204
Location: http://127.0.0.1:8181/files/splash.html?url=aHR0cDovL3d3dy5nZ2cuY29tL3NvbWV0aGluZy5odG1s

Redirect to: <a href='http://127.0.0.1:8181/files/splash.html?url=aHR0cDovL3d3dy5nZ2cuY29tL3NvbWV0aGluZy5odG1s'>http://127.0.0.1:8181/files/splash.html?url=aHR0cDovL3d3dy5nZ2cuY29tL3NvbWV0aGluZy5odG1s</a>
```

With the PAC file, make sure you exclude the service from being proxied!!! Otherwise your browser will be looping trying to access splash page from the service.

```
function FindProxyForURL(url, host) {
  if (isPlainHostName(host) ||
      shExpMatch(host, "*.local") ||
      isInNet(dnsResolve(host), "10.0.0.0", "255.0.0.0") ||
      isInNet(dnsResolve(host), "172.16.0.0",  "255.240.0.0") ||
      isInNet(dnsResolve(host), "192.168.0.0",  "255.255.0.0") ||
      isInNet(dnsResolve(host), "127.0.0.0", "255.255.255.0") ||
      (host == "127.0.0.1"))   // <<<--- THIS ONE  !!!
        return "DIRECT";
 
  if (url.substring(0, 5) == "http:") {
    return "PROXY 127.0.0.1:3000";
  }
  return "DIRECT";
}
```

NOTE: Yes, `127.0.0.1` is already excluded from being proxied, it's just a placeholder in case you plan to host this proxy elsewhere.

## Your browser

Configure your browser's PAC file path to point at `http://127.0.0.1:8181/files/proxy.pac`. 

NOTE: Change `127.0.0.1` if you host this proxy elsewhere.
