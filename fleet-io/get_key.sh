#!/bin/bash
certs=`openssl s_client -servername 127.0.0.1 -host 127.0.0.1 -port 1234 -showcerts </dev/null 2>/dev/null | sed -n '/Certificate chain/,/Server certificate/p'`

rest=$certs
while [[ "$rest" =~ '-----BEGIN CERTIFICATE-----' ]]
do
 cert="${rest%%-----END CERTIFICATE-----*}-----END CERTIFICATE-----"
 rest=${rest#*-----END CERTIFICATE-----} echo `echo "$cert" | grep 's:' | sed 's/.*s:\(.*\)/\1/'` 
 
 echo "$cert" | openssl x509 -pubkey -noout | 
    openssl rsa -pubin -outform der 2>/dev/null | 
    openssl dgst -sha256 -binary | openssl enc -base64
done
