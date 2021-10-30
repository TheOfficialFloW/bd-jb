OUTPUT_FORMAT("elf64-x86-64", "elf64-x86-64", "elf64-x86-64")
OUTPUT_ARCH(i386:x86-64)

ENTRY(_start)

SECTIONS
{
  . = 0x916300000;
  .text   : { *(.text     .text.*) }
  . = 0x916304000;
  .rodata : { *(.rodata   .rodata.*) }
  .data   : { *(.data     .data.*) }
  .bss    : { *(.bss      .bss.*) }
}
