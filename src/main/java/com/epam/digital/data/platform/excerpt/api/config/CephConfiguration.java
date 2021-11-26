/*
 * Copyright 2021 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.excerpt.api.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.integration.ceph.service.impl.CephServiceS3Impl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CephConfiguration {

  @Bean
  public CephService excerptCephService(
      @Value("${datafactory-excerpt-ceph.http-endpoint}") String uri,
      @Value("${datafactory-excerpt-ceph.access-key}") String accessKey,
      @Value("${datafactory-excerpt-ceph.secret-key}") String secretKey) {
    return new CephServiceS3Impl(cephAmazonS3(uri, accessKey, secretKey));
  }

  @Bean
  public CephService excerptSignatureCephService(
      @Value("${excerpt-signature-ceph.http-endpoint}") String uri,
      @Value("${excerpt-signature-ceph.access-key}") String accessKey,
      @Value("${excerpt-signature-ceph.secret-key}") String secretKey) {
    return new CephServiceS3Impl(cephAmazonS3(uri, accessKey, secretKey));
  }

  @Bean
  public CephService requestSignatureCephService(
      @Value("${request-signature-ceph.http-endpoint}") String uri,
      @Value("${request-signature-ceph.access-key}") String accessKey,
      @Value("${request-signature-ceph.secret-key}") String secretKey
  ) {
    return new CephServiceS3Impl(cephAmazonS3(uri, accessKey, secretKey));
  }

  private AmazonS3 cephAmazonS3(String uri, String accessKey, String secretKey) {

    var credentials =
        new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));

    var clientConfig = new ClientConfiguration();
    clientConfig.setProtocol(Protocol.HTTP);

    return AmazonS3ClientBuilder.standard()
        .withCredentials(credentials)
        .withClientConfiguration(clientConfig)
        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(uri, null))
        .withPathStyleAccessEnabled(true)
        .build();
  }
}
