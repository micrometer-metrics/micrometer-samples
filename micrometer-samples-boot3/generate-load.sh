#! /bin/sh

index=1
while true; do
    if [ "$((index % 50))" -eq '0' ]; then
        name='qwerty';
    elif [ "$((index % 3))" -eq '0' ]; then
        name='mike';
    else
        name='suzy';
    fi

    curl --silent --show-error --output /dev/null "http://localhost:8080/greet/$name"
    index=$((index + 1))
    sleep 0.3;
done
