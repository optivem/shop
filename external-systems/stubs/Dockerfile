FROM wiremock/wiremock:3.10.0@sha256:052f7d05b585d5cc956ecf2f1543b1a721858ec83baf0d11b507a113e3b023ee
COPY . /home/wiremock
EXPOSE 8080
CMD ["--verbose", "--global-response-templating"]
