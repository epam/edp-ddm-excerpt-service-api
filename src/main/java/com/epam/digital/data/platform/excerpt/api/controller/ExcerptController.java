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

package com.epam.digital.data.platform.excerpt.api.controller;

import com.epam.digital.data.platform.excerpt.api.annotation.HttpRequestContext;
import com.epam.digital.data.platform.excerpt.api.annotation.HttpSecurityContext;
import com.epam.digital.data.platform.excerpt.api.audit.AuditableController;
import com.epam.digital.data.platform.excerpt.api.model.DetailedErrorResponse;
import com.epam.digital.data.platform.excerpt.api.model.RequestContext;
import com.epam.digital.data.platform.excerpt.api.model.SecurityContext;
import com.epam.digital.data.platform.excerpt.api.service.ExcerptGenerationService;
import com.epam.digital.data.platform.excerpt.api.service.ExcerptRetrievingService;
import com.epam.digital.data.platform.excerpt.api.service.ExcerptStatusCheckService;
import com.epam.digital.data.platform.excerpt.model.ExcerptEntityId;
import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.epam.digital.data.platform.excerpt.model.StatusDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

@Tag(description = "Excerpts management service Rest API", name = "excerpts-service-api")
@RestController
@RequestMapping("/excerpts")
public class ExcerptController {

  private static final String CONTENT_DISPOSITION_HEADER_NAME = "Content-Disposition";
  private static final String ATTACHMENT_HEADER_VALUE = "attachment; filename=\"%s.%s\"";

  private final Logger log = LoggerFactory.getLogger(ExcerptController.class);

  private final ExcerptGenerationService excerptGenerationService;
  private final ExcerptRetrievingService excerptRetrievingService;
  private final ExcerptStatusCheckService excerptStatusCheckService;

  public ExcerptController(
      ExcerptGenerationService excerptGenerationService,
      ExcerptRetrievingService excerptRetrievingService,
      ExcerptStatusCheckService excerptStatusCheckService) {
    this.excerptGenerationService = excerptGenerationService;
    this.excerptRetrievingService = excerptRetrievingService;
    this.excerptStatusCheckService = excerptStatusCheckService;
  }

  @Operation(
      summary = "Create an excerpt generation record",
      description = "### Endpoint purpose:\n  Creates an excerpt generation record by sending required parameters as JSON data. Returns the UUID of the generated excerpt, which can be used to access the generated document.\n ### Authorization:\n This endpoint requires valid user authentication. To access this endpoint, the request must include a valid access token in the _X-Access-Token_ header, otherwise, the API will return a _401 Unauthorized_ status code",
      parameters = @Parameter(
          in = ParameterIn.HEADER,
          name = "X-Access-Token",
          description = "Token used for endpoint security",
          required = true,
          schema = @Schema(type = "string")
      ),
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ExcerptEventDto.class),
              examples = {
                  @ExampleObject(value = "{\n" +
                      "  \"excerptType\": \"subject-laboratories-accreditation-excerpt\",\n" +
                      "  \"requiresSystemSignature\": true,\n" +
                      "  \"excerptInputData\": {\n" +
                      "    \"subjectId\": \"<UUID>\"\n" +
                      "  }\n" +
                      "}"
                  )
              })
      ),
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "OK. Excerpt ID successfully generated.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ExcerptEntityId.class),
                  examples = {
                      @ExampleObject(value = "{\n" +
                          "  \"excerptIdentifier\": \"<UUID>\"\n" +
                          "}"
                      )
                  })
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad Request. Invalid excerpt type or incorrect request parameters.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized. Missing or invalid access token or digital signature.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal Server Error. Error occurred during the excerpt generation process.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
      })
  @AuditableController(action = "GENERATE EXCERPT CALL")
  @PostMapping
  public ResponseEntity<ExcerptEntityId> generate(
      @Valid @RequestBody ExcerptEventDto excerptEventDto,
      @HttpRequestContext RequestContext requestContext,
      @HttpSecurityContext SecurityContext securityContext) {
    log.info("Excerpt generation called");
    return ResponseEntity.ok()
        .body(excerptGenerationService.generateExcerpt(excerptEventDto, requestContext, securityContext));
  }

  @Operation(
      summary = "Retrieve an excerpt file",
      description = "### Endpoint purpose:\n This endpoint allows users to download an excerpt file based on the provided excerpt ID. Returns the excerpt file as a downloadable resource.\n ### Authorization:\n This endpoint requires valid user authentication. To access this endpoint, the request must include a valid access token in the _X-Access-Token_ header, otherwise, the API will return a _401 Unauthorized_ status code. \n ### Validation: During excerpt creation, the system performs validation of the digital signature if enabled, and validation of the template associated with the excerpt type. If these validations fail, an exception is thrown. If all input data is correct, a new excerpt is created and its ID is returned in the response. \n ### Validation: During excerpt creation, the system performs validation of the digital signature if enabled, and validation of the template associated with the excerpt type. If these validations fail, an exception is thrown. If all input data is correct, a new excerpt is created and its ID is returned in the response.",
      parameters = {
          @Parameter(
              in = ParameterIn.HEADER,
              name = "X-Access-Token",
              description = "Token used for endpoint security",
              required = true,
              schema = @Schema(type = "string")
          ),
          @Parameter(
              in = ParameterIn.PATH,
              name = "excerptId",
              description = "The UUID of the excerpt to retrieve",
              required = true,
              schema = @Schema(type = "string")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "OK. Excerpt file successfully retrieved.",
              content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad Request. Invalid request parameters or data.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized. Missing or invalid access token.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal Server Error. Error occurred while retrieving the excerpt.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      }
  )
  @AuditableController(action = "RETRIEVE EXCERPT CALL")
  @GetMapping("/{id}")
  public ResponseEntity<Resource> retrieve(
      @PathVariable("id") UUID id, @HttpSecurityContext SecurityContext securityContext) {
    log.info("Excerpt retrieval called");

    var excerpt = excerptRetrievingService.getExcerpt(id, securityContext);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(excerpt.getCephObject().getMetadata().getContentLength())
        .header(CONTENT_DISPOSITION_HEADER_NAME,
            String.format(ATTACHMENT_HEADER_VALUE, id.toString(), excerpt.getExcerptType()))
        .body(new InputStreamResource(excerpt.getCephObject().getContent()));
  }

  @Operation(
      summary = "Get the status of an excerpt generation",
      description = "### Endpoint purpose: \n This endpoint is used for getting the status of an excerpt generation based on the provided excerpt ID. Returns the status of the generation as a JSON object.\n ### Authorization:\n This endpoint requires valid user authentication. To access this endpoint, the request must include a valid access token in the _X-Access-Token_ header, otherwise, the API will return a _401 Unauthorized_ status code",
      parameters = {
          @Parameter(
              in = ParameterIn.HEADER,
              name = "X-Access-Token",
              description = "Token used for endpoint security",
              required = true,
              schema = @Schema(type = "string")
          ),
          @Parameter(
              in = ParameterIn.PATH,
              name = "excerptId",
              description = "The UUID of the excerpt to retrieve",
              required = true,
              schema = @Schema(type = "string")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "OK. Excerpt generation status successfully retrieved.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = StatusDto.class),
                  examples = {
                      @ExampleObject(value = "{\n" +
                          "  \"status\": \"FAILED\",\n" +
                          "  \"statusDetails\": \"Technical description of the error\"\n" +
                          "}"
                      )
                  })
          ),
          @ApiResponse(
              responseCode = "400",
              description = "Bad Request. Invalid request parameters or data.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "401",
              description = "Unauthorized. Missing or invalid access token.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Not Found. No generation status found for the provided excerpt ID.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal Server Error. Error occurred while retrieving the generation status.",
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = DetailedErrorResponse.class))
          )
      }
  )
  @GetMapping("/{id}/status")
  public ResponseEntity<StatusDto> status(@PathVariable("id") UUID id) {
    log.info("Excerpt status retrieval called");

    var status = excerptStatusCheckService.getStatus(id);
    return ResponseEntity.ok().body(status);
  }
}
