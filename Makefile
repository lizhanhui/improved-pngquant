# Makefile for pngquant
VERSION = $(shell grep 'define PNGQUANT_VERSION' pngquant.c | egrep -Eo '1\.[0-9.]*')

# get the arch
ARCH := $(shell getconf LONG_BIT)

# This changes default "cc" to "gcc", but still allows customization of the CC variable
# if this line causes problems with non-GNU make, just remove it:
CC := $(patsubst cc,gcc,$(CC))

LIB ?= libpngquant.so
PREFIX ?= /usr/local
LIBPREFIX_32 := $(PREFIX)/lib
LIBPREFIX_64 := $(PREFIX)/lib64
LIBPREFIX := $(LIBPREFIX_$(ARCH))

# Alternatively, build libpng and zlib in these directories:
CUSTOMLIBPNG ?= ../libpng
CUSTOMZLIB ?= ../zlib

CFLAGSOPT ?= -O3 -fearly-inlining -fstrict-aliasing -ffast-math -funroll-loops -fomit-frame-pointer -momit-leaf-frame-pointer -ffinite-math-only -fno-trapping-math -funsafe-loop-optimizations -fPIC

CFLAGS ?= -DNDEBUG -Wall -Wno-unknown-pragmas -I. -I$(CUSTOMLIBPNG) -I$(CUSTOMZLIB) -I/System/Library/Frameworks/JavaVM.framework/Headers/ -I/usr/local/include/ -I/usr/include/ -I/usr/X11/include/ -I/opt/local/include/ $(CFLAGSOPT)
CFLAGS += -std=c99 $(CFLAGSADD) -fPIC

LDFLAGS ?= -L$(CUSTOMLIBPNG) -L$(CUSTOMZLIB) -L/opt/local/lib -L/usr/local/lib/ -L/usr/lib/ -L/usr/X11/lib/
LDFLAGS += -lpng -lz -lm $(LDFLAGSADD) -shared

OBJS = pngquant.o rwpng.o pam.o mediancut.o blur.o mempool.o viter.o nearest.o
COCOA_OBJS = rwpng_cocoa.o

DISTFILES = $(OBJS:.o=.c) *.[hm] pngquant.1 Makefile README.md INSTALL CHANGELOG COPYRIGHT
TARNAME = pngquant-$(VERSION)
TARFILE = $(TARNAME)-src.tar.bz2

ifdef USE_COCOA
CFLAGS += -DUSE_COCOA=1
OBJS += $(COCOA_OBJS)
LDFLAGS += -framework Cocoa
endif

all: $(LIB)

openmp::
	$(MAKE) CFLAGSADD=-fopenmp LDFLAGSADD="-Bstatic -lgomp" -j8 $(MAKEFLAGS)

$(LIB): $(OBJS)
	$(CC) $(OBJS) $(LDFLAGS) -o $@

rwpng_cocoa.o: rwpng_cocoa.m
	clang -c $(CFLAGS) -o $@ $<

$(OBJS): pam.h rwpng.h

install: $(LIB)
	install -m 0755 -p -D $(LIB) $(DESTDIR)$(LIBPREFIX)/$(LIB)

uninstall:
	rm -f $(DESTDIR)$(LIBREFIX)/$(LIB)

dist: $(TARFILE)

$(TARFILE): $(DISTFILES)
	rm -rf $(TARFILE) $(TARNAME)
	mkdir $(TARNAME)
	cp $(DISTFILES) $(TARNAME)
	tar -cjf $(TARFILE) --numeric-owner --exclude='._*' $(TARNAME)
	rm -rf $(TARNAME)
	shasum $(TARFILE)

clean:
	rm -f $(LIB) $(OBJS) $(COCOA_OBJS) $(TARFILE)

.PHONY: all openmp install uninstall dist clean
.DELETE_ON_ERROR:
