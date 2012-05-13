require 'net/http'
require 'socket'
require 'timeout'
require 'pp'


fwds = [
        ['127.0.0.1',9147],
        ['127.0.0.1',9148]
       ]

responses = Array.new(fwds.size)
start = true
start_clock = 0
play_clock = 0
threads = []
sockets = []
NET_DELAY = 1

server = TCPServer.new(9145)  
loop {                        
  threads.each{|t| t.kill}
  threads = []
  responses = Array.new(fwds.size){""}
  client = server.accept
  puts "Game state recieved"
  header = ""
  content_length = 0
  while line = client.gets # Read lines from socket
    header << line
    if length = line.match(/Content-Length: (.*)/)
      content_length = length[1].to_i
    end
    if line == "\n"
      break
    end
  end
  body = client.read(content_length)
  if (not body =~ /START/ and start) or (body =~ /START/ and not start)
    puts "WARNING: Unexpected START block"
  end
  if start
    times = body.match(/(\d+) (\d+)\)$/)
    start_clock = times[1].to_i
    play_clock = times[2].to_i
    puts "Play clock: #{play_clock}"
    puts "Start clock: #{start_clock}"
    body.gsub!(/(\d+) (\d+)\)$/, "#{start_clock - NET_DELAY} #{play_clock - NET_DELAY})")
  end

  clock = play_clock
  clock = start_clock if start
  start = false
  puts Time.now
  message = header + body
  begin
  Timeout.timeout(clock - NET_DELAY/2.0){
    fwds.each_index{|f_idx|
      threads << Thread.new{
        f = fwds[f_idx]
        s = TCPSocket.new(*f)
        s.print(message)
        while l = s.gets
          responses[f_idx] << l
        end
        puts "Got response from AI #{f_idx}"
      }
    }
    while responses[0].size == 0
      sleep(0.05)
    end
  }
  rescue Timeout::Error
    puts "Timeout on best algorithm"
    puts Time.now
    # we ran out of time...
    responses.each{|r|
      if r.size > 0
        responses[0] = r
        break
      end
    }
  end
  client.print(responses[0])
  client.close
  if responses[0].size == 0
    puts "Move error"
  else
    puts "Move proxied"
  end
  STDOUT.flush
}
