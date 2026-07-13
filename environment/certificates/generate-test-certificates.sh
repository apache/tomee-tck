#!/bin/sh

set -eu

output_dir=${1:-target/ee11-certificates}
password=changeit

mkdir -p "$output_dir"
for file in server.p12 server.cer server-truststore.p12 clientcert.p12 cts_cert.cer client-truststore.p12; do
  if [ -e "$output_dir/$file" ]; then
    echo "refusing to overwrite $output_dir/$file" >&2
    exit 1
  fi
done

keytool -genkeypair -noprompt \
  -alias server -keyalg RSA -keysize 2048 -validity 365 \
  -dname "CN=localhost, OU=Apache TomEE, O=Apache Software Foundation" \
  -ext "SAN=dns:localhost,ip:127.0.0.1" \
  -storetype PKCS12 -keystore "$output_dir/server.p12" \
  -storepass "$password" -keypass "$password"

keytool -exportcert -rfc -alias server \
  -keystore "$output_dir/server.p12" -storepass "$password" \
  -file "$output_dir/server.cer"

# These client-certificate defaults come from the Jakarta EE 11 TCK guide.
keytool -genkeypair -noprompt \
  -alias cts -keyalg RSA -keysize 2048 -validity 365 \
  -dname "CN=CTS, OU=Eclipse Foundation, O=Jakarta EE" \
  -storetype PKCS12 -keystore "$output_dir/clientcert.p12" \
  -storepass "$password" -keypass "$password"

keytool -exportcert -rfc -alias cts \
  -keystore "$output_dir/clientcert.p12" -storepass "$password" \
  -file "$output_dir/cts_cert.cer"

keytool -importcert -noprompt -alias cts \
  -file "$output_dir/cts_cert.cer" \
  -storetype PKCS12 -keystore "$output_dir/server-truststore.p12" \
  -storepass "$password"

keytool -importcert -noprompt -alias server \
  -file "$output_dir/server.cer" \
  -storetype PKCS12 -keystore "$output_dir/client-truststore.p12" \
  -storepass "$password"

echo "generated test-only TLS material in $output_dir"
