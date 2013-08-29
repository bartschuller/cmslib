package views.html.helper

package object bootstrap30 {
  implicit val twitterBootstrapField = new FieldConstructor {
    def apply(elts: FieldElements) = bootstrap30FieldConstructor(elts)
  }

}
