#include <string>
#include <algorithm>
#include <utility>

#include <cstring>
#include <cstddef>
#include <cerrno>
#include <syslog.h>

#include <sstream>
#include <string.h>

#include <vector>
#include <climits>
#include <memory>

extern "C" {
#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include "piControl.h"

}

//#ifdef (ANDROID) || defined(__ANDROID__)
//#include "jni.h"
//#else
#include "org_clehne_revpi_dataio_DataInOut.h"
//#endif

static const int ERRNO_BUFFER_LEN = 1024;

typedef unsigned char BYTE;

static void throwException(JNIEnv *env, const std::string& exception_name,
		const std::string& msg) {
	const jclass exception = env->FindClass(exception_name.c_str());
	if (exception == NULL) {
		return;
	}
	env->ThrowNew(exception, msg.c_str());
}

static void throwIOExceptionMsg(JNIEnv *env, const std::string& msg) {
	throwException(env, "java/io/IOException", msg);
}

//static void throwIOExceptionErrno(JNIEnv *env, const int exc_errno) {
//	char message[ERRNO_BUFFER_LEN];
//	const char * const msg = (char *) strerror_r(exc_errno, message,
//			ERRNO_BUFFER_LEN);
//	if (((long) msg) == 0) {
//		// POSIX strerror_r, success
//		throwIOExceptionMsg(env, std::string(message));
//	} else if (((long) msg) == -1) {
//		// POSIX strerror_r, failure
//		// (Strictly, POSIX only guarantees a value other than 0. The safest
//		// way to implement this function is to use C++ and overload on the
//		// type of strerror_r to accurately distinguish GNU from POSIX. But
//		// realistic implementations will always return -1.)
//		snprintf(message, ERRNO_BUFFER_LEN, "errno %d", exc_errno);
//		throwIOExceptionMsg(env, std::string(message));
//	} else {
//		// glibc strerror_r returning a string
//		throwIOExceptionMsg(env, std::string(msg));
//	}
//}

//static void throwIllegalArgumentException(JNIEnv *env,
//		const std::string& message) {
//	throwException(env, "java/lang/IllegalArgumentException", message);
//}
//
//static void throwOutOfMemoryError(JNIEnv *env, const std::string& message) {
//	throwException(env, "java/lang/OutOfMemoryError", message);
//}



void logthis(std::string msg) {
	openlog("librevpi_dio_java native library ", LOG_CONS, LOG_USER);
	std::stringstream strs;
	strs << msg;
	std::string temp_str = strs.str();
	char* char_type = (char*) temp_str.c_str();
	syslog(LOG_INFO, char_type);

}

/***********************************************************************************/
/*!
 * @brief Get Processdata
 *
 * Gets Processdata from a specific position
 *
 * @param[in]   Offset
 * @param[in]   Length
 * @param[out]  pData
 *
 * @return Number of Bytes read or error if negative
 *
 ************************************************************************************/
int piControlRead(jint fd, uint32_t Offset, uint32_t Length, uint8_t * pData)
{
      int BytesRead = 0;

      /* seek */
      if (lseek(fd, Offset, SEEK_SET) < 0) {
         return -errno;
      }

      /* read */
      BytesRead = read(fd, pData, Length);
      if (BytesRead < 0) {
             return -errno;
      }

      return BytesRead;
}


int piControlWrite(jint fd, uint32_t Offset, uint32_t Length, uint8_t * pData)
{
     int BytesWritten = 0;

     /* seek */
     if (lseek(fd, Offset, SEEK_SET) < 0) {
            return -errno;
     }

     /* Write */
     BytesWritten = write(fd, pData, Length);
     if (BytesWritten < 0) {
           return -errno;
     }

     return BytesWritten;
}


int piControlGetBitValue(jint fd, SPIValue * pSpiValue)
{
	if (fd < 0){
		return -ENODEV;
	}
	pSpiValue->i16uAddress += pSpiValue->i8uBit / 8;
	pSpiValue->i8uBit %= 8;

	if (ioctl(fd, KB_GET_VALUE, pSpiValue) < 0){
		return -errno;
	}
	return 0;
}

int piControlSetBitValue(jint fd, SPIValue * pSpiValue)
{
	if (fd < 0){
		return -ENODEV;
	}
	pSpiValue->i16uAddress += pSpiValue->i8uBit / 8;
	pSpiValue->i8uBit %= 8;

	if (ioctl(fd, KB_SET_VALUE, pSpiValue) < 0){
		return -errno;
	}
	return 0;
}

int piControlGetVariableInfo(jint fd, SPIVariable * pSpiVariable)
{
	if (fd < 0){
		return -ENODEV;
	}
	if (ioctl(fd, KB_FIND_VARIABLE, pSpiVariable) < 0){
		return -errno;
	}
	return 0;
}


int readVariableValue(jint fd, char *pszVariableName)
{
	int rc;
	SPIVariable sPiVariable;
	SPIValue sPIValue;
	uint8_t i8uValue = 0;
	uint16_t i16uValue = 0;

	strncpy(sPiVariable.strVarName, pszVariableName, sizeof(sPiVariable.strVarName));
	rc = piControlGetVariableInfo(fd, &sPiVariable);
	if (rc < 0) {
		return rc;
	}
	if (sPiVariable.i16uLength == 1) {
		sPIValue.i16uAddress = sPiVariable.i16uAddress;
		sPIValue.i8uBit = sPiVariable.i8uBit;

		rc = piControlGetBitValue(fd, &sPIValue);
		if (rc < 0){
			return rc;
		}else {
			return sPIValue.i8uValue;
		}

	}
	if (sPiVariable.i16uLength == 8) {
		rc = piControlRead(fd, sPiVariable.i16uAddress, 1, (uint8_t *) & i8uValue);
		if (rc < 0){
	                return rc;
	        }else {
	                return i8uValue;
		}

	}
	if (sPiVariable.i16uLength == 16) {
		rc = piControlRead(fd, sPiVariable.i16uAddress, 2, (uint8_t *) & i16uValue);
		if (rc < 0){
                       return rc;
                }else {
                        return i16uValue;
		}
	} //TODO add 32
	return -99;
}


int writeVariableValue(jint fd, char *pszVariableName, uint32_t i32uValue)
{
	int rc;
	SPIVariable sPiVariable;
	SPIValue sPIValue;
        uint8_t i8uValue = 0;
        uint16_t i16uValue = 0;

	strncpy(sPiVariable.strVarName, pszVariableName, sizeof(sPiVariable.strVarName));
	rc = piControlGetVariableInfo(fd, &sPiVariable);
	if (rc < 0) {
		return -1;
	}

	if (sPiVariable.i16uLength == 1) {
		sPIValue.i16uAddress = sPiVariable.i16uAddress;
		sPIValue.i8uBit = sPiVariable.i8uBit;
		sPIValue.i8uValue = i32uValue;
		rc = piControlSetBitValue(fd, &sPIValue);
		if (rc < 0){
			return -2;
		}else{
			return 0;
		}
	}
	if (sPiVariable.i16uLength == 8) {
		i8uValue = i32uValue;
		rc = piControlWrite(fd, sPiVariable.i16uAddress, 1, (uint8_t *) & i8uValue);
		if (rc < 0){
			return -2;
		}else{
			return 0;
		}
	}
	if (sPiVariable.i16uLength == 16) {
                i16uValue = i32uValue;
		rc = piControlWrite(fd, sPiVariable.i16uAddress, 2, (uint8_t *) & i16uValue);
		if (rc < 0){
	                return -2;
                }else{
                        return 0;
                }
	}
	return -3;
}




/******************************************************************************/
/*******************************  Functions  **********************************/
/******************************************************************************/


JNIEXPORT jint JNICALL Java_org_clehne_revpi_dataio_DataInOut__1openDIO(
		JNIEnv *env, jclass obj) {
	int PiControlHandle = -1;
	PiControlHandle = open(PICONTROL_DEVICE, O_RDWR);
    if (PiControlHandle < 0) {
    	throwIOExceptionMsg(env, "Could not open revpi datainout.");
    }
    return PiControlHandle;
}


JNIEXPORT void Java_org_clehne_revpi_dataio_DataInOut__1closeDIO
(JNIEnv *env, jclass obj, jint fd)
{
    /* open handle if needed */
    if (fd > 0) {
    	close(fd);
    }else{
    	throwIOExceptionMsg(env, "Could not close. Invalid handle");
    }
	return;
}


JNIEXPORT jint JNICALL Java_org_clehne_revpi_dataio_DataInOut__1setValueDIO
	(JNIEnv *env, jclass obj, jint fd, jstring channelAliasObj, jint value) {
    
	int ret;
    const char *channelName;
    
    channelName = env->GetStringUTFChars( channelAliasObj, NULL ) ;
	ret = writeVariableValue(fd, (char*)channelName, value);
    env->ReleaseStringUTFChars(channelAliasObj, channelName);
    
	return ret;
}

JNIEXPORT jint JNICALL Java_org_clehne_revpi_dataio_DataInOut__1getValueDIO
	(JNIEnv *env, jclass obj, jint fd, jstring channelAliasObj) {

	int ret;
    const char *channelName;
    
    channelName = env->GetStringUTFChars( channelAliasObj, NULL ) ;
	ret = readVariableValue(fd, (char*)channelName);
    env->ReleaseStringUTFChars(channelAliasObj, channelName);
	return ret;
}

