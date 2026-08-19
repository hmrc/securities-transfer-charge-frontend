document.addEventListener("DOMContentLoaded", function () {
    const printLink = document.getElementById("print-this-page")

    if (printLink) {
        printLink.addEventListener("click", function (event) {
            event.preventDefault()
            window.print()
        })
    }
})