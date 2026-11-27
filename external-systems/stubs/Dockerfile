FROM wiremock/wiremock:3.13.2-3@sha256:0d4ecb3e4dc8213fd7a4d37d6a78f6e6b553a6d2e15bd51b0999781282ac61b3
# Build context is external-systems/ (not this folder): the ERP product photos in
# external-systems/erp-images become WireMock body files, served by erp-images.json.
# What is sent to the builder is allowlisted in Dockerfile.dockerignore next to this file.
COPY stubs /home/wiremock
COPY erp-images /home/wiremock/__files/erp/images
EXPOSE 8080
CMD ["--verbose", "--global-response-templating"]
