outputPort Dummy {
  location: "socket://localhost:33333"
  protocol: sodep
  RequestResponse:
    op(void)(void)
}

main {
  scope (Scope1) {
    //install (Err => nullProcess)
    scope (Scope2) {
      scope (Scope3) {
        op@Dummy()()
      }
    }
  }
}