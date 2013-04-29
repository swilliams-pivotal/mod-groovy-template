package io.vertx.mods.template

import groovy.text.Template
import groovy.text.TemplateEngine
import groovy.text.GStringTemplateEngine
import groovy.text.SimpleTemplateEngine
import groovy.text.XmlTemplateEngine
import groovy.transform.CompileStatic

import org.vertx.groovy.core.buffer.Buffer
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.Future

import com.hazelcast.query.Predicates.GetExpressionImpl.ThisGetter;


@CompileStatic
class GroovyTemplateParser extends Verticle {

  public static final String DEFAULT_ADDRESS = 'groovy.template.parser'

  String address
  TemplateEngine engine

  def engines = [
    'gstring': GStringTemplateEngine,
    'simple': SimpleTemplateEngine,
    'xml': XmlTemplateEngine
  ]

  Map<String, Template> templates = [:]

  @Override
  def start(Future result) {
    this.address = container.config['address'] ?: DEFAULT_ADDRESS

    String engineName = container.config['engine'] ?: 'simple'
    Class<TemplateEngine> engineClass = engines[engineName]
    this.engine = engineClass.newInstance()

    String templateDir = container.config['templateDir'] ?: 'templates'
    String suffix = container.config['suffix'] ?: '.html'

    String[] paths = vertx.getFileSystem().readDirSync(templateDir, ".+${suffix}")
    paths.each { String path->
      Buffer b = vertx.getFileSystem().readFileSync(path)
      String templateName = path.replaceAll('.+\\/(.+)' + suffix, '$1')
      String templateText = b.toString()

      Template template = engine.createTemplate(templateText)
      templates.put(templateName, template)
    }

    // use a local handler to reduce IO
    vertx.eventBus.registerLocalHandler(address, this.&parser)
    result.setResult(null)
  }

  @Override
  def stop() {
    vertx.eventBus.unregisterHandler(address, this.&parser)
  }

  def parser(Message msg) {
    def json = msg.body() as Map
    def reply = [:]

    if (json.containsKey('templateName')) {
      Template template = templates.get(json['templateName'])
      def parsed = template.make(json)
      reply.body = parsed.toString()
    }
    else {
      reply.error = "Required key 'templateName' is missing"
    }

    msg.reply(reply)
  }

}
