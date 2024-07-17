package com.example;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisCredentialsProvider;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SslOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import reactor.core.publisher.Mono;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws GeneralSecurityException, IOException 
    {

        String discoveryEndpointIp = "10.128.15.235";
        //String discoveryEndpointIp = "10.128.15.241";
        int discoveryEndpointPort = 6379;
        
        SslOptions sslOptions = SslOptions.builder()
        .jdkSslProvider()
        .trustManager(new File("/home/admin/redis/maven-sample02/demo/server-ca.pem"))
        .build();

        ClusterClientOptions clientOptions = ClusterClientOptions.builder().sslOptions(sslOptions).build();

        // Obtain credentials using Application Default Credentials
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault(); 
        //System.out.println("credentials : " + credentials.getAccessToken().getTokenValue());
        AccessToken accessToken = credentials.refreshAccessToken();
	System.out.println("Access Torken : " + accessToken.getTokenValue());

	RedisCredentialsProvider provider = () -> Mono.just(RedisCredentials.just("default", accessToken.getTokenValue().toCharArray()));
        RedisURI redisUri = RedisURI.Builder.redis(discoveryEndpointIp, discoveryEndpointPort).withSsl(true).withAuthentication(provider).build();
        //RedisURI redisUri = RedisURI.Builder.redis(discoveryEndpointIp, discoveryEndpointPort).withSsl(true).build();

        // Create Redis Cluster Client
        RedisClusterClient clusterClient = RedisClusterClient.create(redisUri);
        clusterClient.setOptions(clientOptions);

        try (// Establish connection to Redis Cluster
            StatefulRedisClusterConnection connection = clusterClient.connect()) {
            // Retrieve synchronous Redis Cluster commands
            RedisAdvancedClusterCommands syncCommands = connection.sync();

            // Perform Redis operations
            syncCommands.set("key", "Hello, Redis!");
            System.out.println("key : " + syncCommands.get("key"));
            // Close the connection and shutdown the client
            connection.close();
        }

        clusterClient.shutdown();
    }

}

