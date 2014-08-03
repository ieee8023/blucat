#/bin/bash

while read input 
do
    echo $input
    if [[ "$input" == "f"* ]];
    then
	echo "Forward"
	sh key-mac.sh 124
    fi

    if [[ "$input" == "b"* ]];
    then
        echo "Backward"
	sh key-mac.sh 123
    fi

    if [[ "$input" == "say"* ]];
    then
        echo "Say $input"
        say `echo $input | sed s/say//`
    fi

	

done