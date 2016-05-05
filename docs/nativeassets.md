Native Assets (as opposed to Native Tokens)
-------------------------------------------

Native Assets are a way to represent different asset types (such as stocks or property titles) in a blockchain.  
Instead of a single implied asset type (the "Native Token" for that blockchain), there is an explicit 256-bit 
Asset ID stored with each output's CTxOut object which represents a certain asset.
(Note the distinction between 'Native Token' and 'Native Asset'.) 

Unless the Asset ID specifies that the output is the blockchain's 'Native Token', the quantity in the 
output represents only the Native Asset and carries 0 value of the Native Token.

Outputs with different AssetIDs (that is, representing different assets) cannot be combined or summed 
together, but can be atomically transferred together in the same transaction.  

Assets can be introduced into the blockchain by any participant by creating a transaction with a new Asset 
ID that is unique in that blockchain; this process is called an "issuance".  In the near future, it will 
be possible to issue additional quantities of an Asset ID already present in the blockchain, but this is currently 
not supported.

As alluded to above, if you wish to use a purely 'native token' in the Hyperledger blockchain and 
not correlate a 'Native Asset' to the output, by convention we currently use an Asset ID 
where all bytes are 0 except the last, "1". Since this is stored in little-endian
representation, it looks like 0x0100000000... .  Note that we may change this to be the 
transaction ID of the genesis block in order to conform to Element's implementation. 

Note that Elements, which implements Native Assets slightly differently, uses only the genesis 
block transaction ID without the output index, (and not the Asset ID explained above) to 
represent purely native tokens.  Also, in Elements, 'Native Tokens' are known as 'Fee Assets'.
