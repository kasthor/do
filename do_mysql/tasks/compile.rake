begin
  gem('rake-compiler')
  require 'rake/extensiontask'
  require 'rake/javaextensiontask'

  # Hack to avoid "allocator undefined for Proc" issue when unpacking Gems:
  # gemspec provided by Jeweler uses Rake::FileList for files, test_files and
  # extra_rdoc_files, and procs cannot be marshalled.
  def gemspec
    @clean_gemspec ||= eval("#{Rake.application.jeweler.gemspec.to_ruby}") # $SAFE = 3\n
  end

  Rake::ExtensionTask.new('do_mysql_ext', gemspec) do |ext|

    mysql_lib = File.expand_path(File.join(File.dirname(__FILE__), '..', 'vendor', "mysql-#{BINARY_VERSION}-win32"))

    # automatically add build options to avoid need of manual input
    if RUBY_PLATFORM =~ /mswin|mingw/ then
      ext.config_options << "--with-mysql-include=#{mysql_lib}/include"
      ext.config_options << "--with-mysql-lib=#{mysql_lib}/lib/opt"
    else
      ext.cross_compile = true
      ext.cross_platform = ['x86-mingw32', 'x86-mswin32-60']
      ext.cross_config_options << "--with-mysql-include=#{mysql_lib}/include"
      ext.cross_config_options << "--with-mysql-lib=#{mysql_lib}/lib/opt"
    end

  end

  Rake::JavaExtensionTask.new('do_mysql_ext', gemspec) do |ext|
    ext.ext_dir   = 'ext-java/src/main/java'
    ext.classpath = '../do_jdbc/lib/do_jdbc_internal.jar'
    ext.java_compiling do |gem|
      gem.add_dependency 'jdbc-mysql', '>=5.0.4'
      gem.add_dependency 'do_jdbc',    '0.10.1'
      gem.post_install_message = <<EOF
==========================================================================

  DataObjects MySQL Driver:
    You've installed the binary extension for JRuby (Java platform)

==========================================================================
EOF
      # components, files and paths
      # gem.files = Dir['lib/**/*.rb', 'spec/**/*.rb', 'tasks/**/*.rake',
      #              'LICENSE', 'Rakefile', '*.{markdown,rdoc,txt,yml}', 'lib/*.jar']
    end
  end
rescue LoadError
  warn "To compile, install rake-compiler (gem install rake-compiler)"
end