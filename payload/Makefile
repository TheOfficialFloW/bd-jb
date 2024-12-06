TARGET = payload
OBJS = start.o payload.o

CC = gcc
OBJCOPY = objcopy
CFLAGS = -DSMP -isystem freebsd-headers/include -Wl,--build-id=none -Os -fno-stack-protector -fcf-protection=none -fpic -fpie
LDFLAGS = -T linker.ld -nostartfiles -nostdlib

all: $(TARGET).bin

%.bin: %.elf
	$(OBJCOPY) -S -O binary $^ $@

$(TARGET).elf: $(OBJS)
	$(CC) $(CFLAGS) $^ -o $@ $(LDFLAGS)

clean:
	@rm -f $(TARGET).bin $(TARGET).elf $(OBJS)
