# SocraticBliss (R)
import os, sys

def read_at(fp, off, len):
  fp.seek(off)
  return fp.read(len)

def main(argc, argv):
  if argc != 2:
    print('Usage: %s 80000000000000XX' % argv[0])
    return 1
  try: 
    with open(sys.argv[1], "rb") as file:
      # Determine if this is a Common or Personal Ticket Blob
      if sys.argv[1].upper() == ('80000000000000E1'):
        ticketType = 'common'
      elif sys.argv[1].upper() == ('80000000000000E2'):
        ticketType = 'personal'
      else:
        ticketType = 'unknown'
          
      # Remove previous entries
      try:
        os.remove('%s_ticketblob.bin' % (ticketType))
      except OSError:
        pass

      count = 0
      fileSize = os.path.getsize(argv[1])  
      
	  # Find first occurance of a Ticket
      for x in xrange(0, fileSize, 0x100):
        if read_at(file, x, 4) == b"\x04\x00\x01\x00":
          ticketStart = x
          break
      
      # Iterate through the Ticket Blob
      for i in xrange(ticketStart, fileSize, 0x400):
        if read_at(file, i, 4) == b"\x04\x00\x01\x00":
          count += 1
          tik_block = read_at(file, i, 0x400)          
          with open('%s_ticketblob.bin' % (ticketType), 'a+b') as outfile:
            outfile.write(tik_block)
  except:
    print('Failed to open %s!' % argv[1])
    return 1
  print('Saved all %d tickets to %s_ticketblob.bin' % (count, ticketType))
  return 0

if __name__=='__main__':
  sys.exit(main(len(sys.argv), sys.argv))
