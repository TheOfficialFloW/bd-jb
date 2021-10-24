TARGET = payload
OBJS = start.o payload.o

CC = gcc
AS = as
OBJCOPY = objcopy
CFLAGS = -Os -fno-stack-protector
LDFLAGS = -T linker.x -nostdlib -nostartfiles

all: $(TARGET).bin

%.bin: %.elf
	$(OBJCOPY) -S -j .text -j .data -j .rodata -j .bss -O binary $^ $@

$(TARGET).elf: $(OBJS)
	$(CC) $(CFLAGS) $^ -o $@ $(LDFLAGS)

clean:
	@rm -f $(TARGET).bin $(TARGET).elf $(OBJS)
