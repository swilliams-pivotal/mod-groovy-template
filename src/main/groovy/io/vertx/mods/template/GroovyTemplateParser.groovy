/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

/**
 * @author swilliams
 *
 */
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
