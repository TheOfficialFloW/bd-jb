TARGET = payload
OBJS = start.o payload.o

CC = gcc
OBJCOPY = objcopy
CFLAGS = -isystem freebsd-headers/include -Os -fno-stack-protector
LDFLAGS = -T linker.x -nostdlib -nostartfiles

all: $(TARGET).bin

%.bin: %.elf
	$(OBJCOPY) -S -O binary $^ $@

$(TARGET).elf: $(OBJS)
	$(CC) $(CFLAGS) $^ -o $@ $(LDFLAGS)

clean:
	@rm -f $(TARGET).bin $(TARGET).elf $(OBJS)
