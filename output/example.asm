section .data
    newline db 10, 0
    input_buffer times 32 db 0
    format_int db '%d', 0
    format_str db '%s', 0
    format_input db '%31s', 0
    temp_str times 32 db 0
    wifi_connecting db 'Conectando a WiFi...', 10, 0
    wifi_success db 'WiFi conectado exitosamente', 10, 0
    wifi_error db 'Error al conectar WiFi', 10, 0
    wifi_prompt_ssid db 'Ingrese SSID: ', 0
    wifi_prompt_pass db 'Ingrese password: ', 0
    ssid_buffer times 64 db 0
    pass_buffer times 64 db 0
    netsh_template db 'netsh wlan connect name="%s"', 0
    netsh_command times 128 db 0

section .bss
    pass resd 1
    Sheyla resd 1
    y resd 1

section .text
extern printf
extern sprintf
extern system
extern scanf
extern atoi
extern exit
extern time
extern srand
extern rand
global main

main:
    push ebp
    mov ebp, esp
    sub esp, 64    ; Espacio para variables locales y temporales

    ; (ASSIGN, 12345678911, null, pass)
    mov dword [pass], 12345678911
    ; (ARG, Sheyla, 0, null)
    push dword [Sheyla]
    ; (ARG, pass, 1, null)
    push dword [pass]
    ; (CALL, _wifi_connect, 2, t0)
    call _wifi_connect
    mov [ebp-4], eax
    ; (INPUT, , null, y)
    push input_buffer
    push format_input
    call scanf
    add esp, 8
    push input_buffer
    call atoi
    add esp, 4
    mov [y], eax

    ; Salir del programa
    push 0
    call exit

; ===== FUNCIÓN NATIVA: _wifi_connect =====
_wifi_connect:
    push ebp
    mov ebp, esp
    sub esp, 16    ; Espacio para variables locales

    push wifi_connecting
    push format_str
    call printf
    add esp, 8

    push wifi_prompt_ssid
    push format_str
    call printf
    add esp, 8
    push ssid_buffer
    push format_input
    call scanf
    add esp, 8

    push ssid_buffer
    push netsh_template
    push netsh_command
    call sprintf
    add esp, 12

    push netsh_command
    call system
    add esp, 4

    push wifi_success
    push format_str
    call printf
    add esp, 8
    mov eax, 1    ; Retornar 1 (éxito)

    mov esp, ebp
    pop ebp
    ret
; ===== FIN DE _wifi_connect =====


