package io.vertx.mods.template

import static org.vertx.testtools.VertxAssert.*
import org.vertx.java.core.AsyncResult
import org.vertx.java.core.Handler
import org.vertx.java.core.eventbus.Message
import org.vertx.java.core.json.JsonObject
import org.vertx.testtools.TestVerticle

import org.junit.Test

/**
 * @author swilliams
 *
 */
class GroovyTemplateParserTest extends TestVerticle {

  @Test
  public void loadSimpleEngine() {
    def config = [
      engine: 'simple',
      suffix: '.html',
      templateDir: 'src/test/resources/templates'
    ]
    container.deployVerticle('groovy:' + GroovyTemplateParser.name, new JsonObject(config), 1, { AsyncResult ar->

      if (ar.failed()) {
        fail(ar.cause().getMessage())
      }

      def msg = [
        templateName: 'simple',
        world: 'world'
      ]
      vertx.eventBus().send(GroovyTemplateParser.DEFAULT_ADDRESS, new JsonObject(msg), { Message reply->

        Map body = reply.body().toMap()
        assertTrue(body.containsKey('body'))

        String text = body.get('body')
        assertEquals('''<!DOCTYPE html>
<html>
  <body>
    <p>Hello world</p>
  </body>
</html>
''', text)

        testComplete()
      } as Handler)

    } as Handler)
  }

  @Test
  public void loadGStringEngine() {
    def config = [
      engine: 'gstring',
      suffix: '.ghtml',
      templateDir: 'src/test/resources/templates'
    ]
    container.deployVerticle('groovy:' + GroovyTemplateParser.name, new JsonObject(config), 1, { AsyncResult ar->

      if (ar.failed()) {
        fail(ar.cause().getMessage())
      }

      def msg = [
        templateName: 'simple',
        world: 'world'
      ]
      vertx.eventBus().send(GroovyTemplateParser.DEFAULT_ADDRESS, new JsonObject(msg), { Message reply->

        Map body = reply.body().toMap()
        assertTrue(body.containsKey('body'))

        String text = body.get('body')
        assertEquals('''<!DOCTYPE html>
<html>
  <body>
    <p>Hello world</p>
  </body>
</html>
''', text)

        testComplete()
      } as Handler)

    } as Handler)
  }

  @Test
  public void loadXMLEngine() {
    def config = [
      engine: 'xml',
      suffix: '.xml',
      templateDir: 'src/test/resources/templates'
    ]
    container.deployVerticle('groovy:' + GroovyTemplateParser.name, new JsonObject(config), 1, { AsyncResult ar->

      if (ar.failed()) {
        fail(ar.cause().getMessage())
      }

      def msg = [
        templateName: 'simple',
        world: 'world'
      ]
      vertx.eventBus().send(GroovyTemplateParser.DEFAULT_ADDRESS, new JsonObject(msg), { Message reply->
        Map body = reply.body().toMap()
        assertTrue(body.containsKey('body'))

        String text = body.get('body')
        assertEquals('''<document>
  Hello world
</document>
''', text)

        testComplete()
      } as Handler)

    } as Handler)
  }

}
