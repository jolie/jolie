constants {
    INIT_CONSTANT = 4,
    MAIN_CONSTANT = 5
}

init {
    a.b = 1
    a.c = 2
    a.d = 3
    INIT_CONSTANT << a
}

main {
    MAIN_CONSTANT = a
}