Gem::Specification.new do |s|
  # basic information
  s.name        = "do_sqlite3"
  s.version     = '0.10.1'

  # description and details
  s.summary     = 'DataObjects Sqlite3 Driver'
  s.description = "Implements the DataObjects API for Sqlite3"

  # dependencies
  s.add_dependency "addressable", "~>2.1"
  s.add_dependency "data_objects", '0.10.1'

  if JRUBY
    s.add_dependency "jdbc-sqlite3", ">=3.5.8"
    s.add_dependency "do_jdbc", '0.10.1'
    s.platform = "java"
    # components, files and paths
    s.files = FileList["lib/**/*.rb", "spec/**/*.rb", "tasks/**/*.rake",
                        "LICENSE", "Rakefile", "*.{markdown,rdoc,txt,yml}", "lib/*.jar"]
  else
    s.platform    = Gem::Platform::RUBY
    s.extensions << 'ext/do_sqlite3_ext/extconf.rb'
    # components, files and paths
    s.files = FileList["lib/**/*.rb", "spec/**/*.rb", "tasks/**/*.rake", "ext/**/*.{rb,c,h}",
                        "LICENSE", "Rakefile", "*.{markdown,rdoc,txt,yml}"]
  end

  # development dependencies
  s.add_development_dependency 'bacon', '~>1.1'


  s.require_path = 'lib'

  # documentation
  s.has_rdoc = false

  # project information
  s.homepage          = 'http://github.com/datamapper/do'
  s.rubyforge_project = 'dorb'

  # author and contributors
  s.author      = 'Dirkjan Bussink'
  s.email       = 'd.bussink@gmail.com'
end
