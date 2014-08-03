
PATH=$PATH:$(pwd)/../


#blucat -k -v -url btspp://00000000CAFE:4 | /bin/bash dispatcher.sh


#blucat -v -url btspp://106B3F598356:4 -e "/bin/bash $(pwd)/dispatcher.sh"

blucat -v -url btspp://106B3F598356:4 | /bin/bash $(pwd)/dispatcher.sh
