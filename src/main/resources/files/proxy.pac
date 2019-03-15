function FindProxyForURL(url, host) {
  if (isPlainHostName(host) ||
      shExpMatch(host, "*.local") ||
      isInNet(dnsResolve(host), "10.0.0.0", "255.0.0.0") ||
      isInNet(dnsResolve(host), "172.16.0.0",  "255.240.0.0") ||
      isInNet(dnsResolve(host), "192.168.0.0",  "255.255.0.0") ||
      isInNet(dnsResolve(host), "127.0.0.0", "255.255.255.0") ||
      (host == "127.0.0.1"))
        return "DIRECT";
 
  if (url.substring(0, 5) == "http:") {
        return "PROXY 127.0.0.1:3000";
  }
  return "DIRECT";
}