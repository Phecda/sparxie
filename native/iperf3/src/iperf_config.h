/*
 * Sparxie mobile build configuration for the vendored iperf 3.21 sources.
 *
 * Keep this conservative: only enable features needed by the mobile
 * client/server integration.
 */
#ifndef IPERF_CONFIG_H
#define IPERF_CONFIG_H

#define HAVE_CLOCK_GETTIME 1
#define HAVE_GETLINE 1
#define HAVE_INTTYPES_H 1
#define HAVE_MSG_TRUNC 1
#define HAVE_NANOSLEEP 1
#define HAVE_POLL_H 1
#define HAVE_PTHREAD 1
#define HAVE_SOCKET_SHUTDOWN_SHUT_WR 1
#define HAVE_STDATOMIC_H 1
#define HAVE_STDINT_H 1
#define HAVE_STDIO_H 1
#define HAVE_STDLIB_H 1
#define HAVE_STRINGS_H 1
#define HAVE_STRING_H 1
#define HAVE_SYS_SOCKET_H 1
#define HAVE_SYS_STAT_H 1
#define HAVE_SYS_TYPES_H 1
#define HAVE_UNISTD_H 1
#define STDC_HEADERS 1

#if defined(__ANDROID__)
#define HAVE_ENDIAN_H 1
#define HAVE_LINUX_TCP_H 1
#endif

#if defined(__APPLE__)
#define HAVE_SYS_ENDIAN_H 1
#endif

#define LT_OBJDIR ".libs/"
#define PACKAGE "iperf"
#define PACKAGE_BUGREPORT "https://github.com/esnet/iperf"
#define PACKAGE_NAME "iperf"
#define PACKAGE_STRING "iperf 3.21"
#define PACKAGE_TARNAME "iperf"
#define PACKAGE_URL "https://software.es.net/iperf/"
#define PACKAGE_VERSION "3.21"
#define VERSION "3.21"

#endif
