# Revolution Pi DataInOut JNI wrapper

Simple JNI wrapper to access the RevolutionPi DigitalIO hardware enhancement board from the RevolutionPi base board within Java  

## Getting Started

These instructions will get you a copy of the project on your rev pi and build the library on that controller.

## Built Instructions

* login to your RevolutionPi and install git tools
* git clone [https://github.com/clehne/librevpi-dio-java](https://github.com/clehne/librevpi-dio-java)
* cd librevpi-dio-java  
* make clean
* make all
# do the following export before check
#export LD_LIBRARY_PATH=<path>/librevpi-dio-java/lib/:$LD_LIBRARY_PATH
* make check

## Deployment

Use the resulting library [./dist/librevpi-dio-java.jar](./dist/librevpi-dio-java.jar) for your needs.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Contributing

* The basic project sceleton is copied from [waal70 - libsocket-can-java.git](https://github.com/waal70/libsocket-can-java.git)
* The real RevolutionPi hardware access code is taken from [RevPi - https://github.com/RevolutionPi/piControl](https://github.com/RevolutionPi/piControl)
* This library was developed to be part of the great and inspiring [openems](https://openems.io/) project. 

## Authors

* **C. Lehne** - *initial* - 


## Acknowledgments

* Hat tip to anyone whose code was used
