Signature Covers Input Value (SCIV)
-----------------------------------

When SCIV is enabled, the signatures verified by the CHECKSIG operators cover not only the
input values that Bitcoin Core normally signs (the previous output, the script, and the 
sequence number, moduli its HashType options), but also allows signing the quantity of 
Native Assets (the 'Input Value', see below) being transferred from the outpoint to the related 
inpoint.
  
This enables hardware signing devices to sign transactions without knowing the full 
previous transactions whose outputs are spent. If the hardware device is mistaken
about the input value, the resulting signature will be invalid.

