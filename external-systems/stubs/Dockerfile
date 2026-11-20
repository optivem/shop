FROM wiremock/wiremock:3.13.2-3@sha256:0d4ecb3e4dc8213fd7a4d37d6a78f6e6b553a6d2e15bd51b0999781282ac61b3
COPY . /home/wiremock
EXPOSE 8080
CMD ["--verbose", "--global-response-templating"]
