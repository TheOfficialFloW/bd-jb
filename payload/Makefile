TARGET = payload
OBJS = start.o stubs.o payload.o resolve.o kernel.o

CC = gcc
OBJCOPY = objcopy
CFLAGS = -isystem sdk/include -Wl,--build-id=none -Os -Wno-builtin-declaration-mismatch -fno-stack-protector -fcf-protection=none -fpic -fpie
LDFLAGS = -T linker.ld -nostartfiles -nostdlib

all: $(TARGET).bin

%.bin: %.elf
	$(OBJCOPY) -S -O binary $^ $@

$(TARGET).elf: $(OBJS)
	$(CC) $(CFLAGS) $^ -o $@ $(LDFLAGS)

clean:
	@rm -f $(TARGET).bin $(TARGET).elf $(OBJS)
