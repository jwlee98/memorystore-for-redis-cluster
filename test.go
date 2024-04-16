package main

import (
  "context"
  "crypto/tls"
  "crypto/x509"
  "io/ioutil"
  "fmt"
  "log"
  "time"

  "github.com/redis/go-redis/v9"
  "golang.org/x/oauth2/google"
)

func main() {
        // Load CA cert
        caFilePath := "./server-ca.pem"
        caCert, err := ioutil.ReadFile(caFilePath)
        if err != nil {
                log.Fatal(err)
        }
        caCertPool := x509.NewCertPool()
        caCertPool.AppendCertsFromPEM(caCert)
      
       ctx := context.Background()
 
        // Obtain credentials using Application Default Credentials
        creds, err := google.FindDefaultCredentials(ctx, "https://www.googleapis.com/auth/cloud-platform")
        if err != nil {
            log.Fatalf("Failed to obtain credentials: %v", err)
        }
        ts := creds.TokenSource
        token, err := ts.Token()
        if err != nil {
            log.Fatalf("Failed to get token: %v", err)
        }
        fmt.Println("AccessToken:", token.AccessToken)


	  // Setup Redis Connection pool
        client := redis.NewClusterClient(&redis.ClusterOptions{
                Addrs:          []string{"10.128.15.235:6379"},
                TLSConfig:      &tls.Config{
                                RootCAs: caCertPool,
                        },
                Username:       "",
                Password:       token.AccessToken,
                DialTimeout:    30 * time.Second, // 연결 타임아웃 설정 (예: 5초)
                ReadTimeout:    10 * time.Second, // 읽기 타임아웃 설정 (예: 3초)
                WriteTimeout:   10 * time.Second, // 쓰기 타임아웃 설정 (예: 3초)
                ReadOnly:       false,
                RouteRandomly:  false,
                RouteByLatency: false,
        })

        err = client.Set(ctx, "key", "value", 0).Err()
        if err != nil {
                log.Fatal(err)
        }
        val, err := client.Get(ctx, "key").Result()
        if err != nil {
             log.Fatal(err)
        }
        fmt.Println("key", val)

}

