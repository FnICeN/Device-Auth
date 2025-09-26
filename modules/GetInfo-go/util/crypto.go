package util

import (
	"encoding/base64"
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/google/tink/go/aead"
	"github.com/google/tink/go/insecurecleartextkeyset"
	"github.com/google/tink/go/keyset"
	"github.com/google/tink/go/signature"
)

type Crypto struct {
	privateKeysetHandle *keyset.Handle
	publicKeysetHandle  *keyset.Handle
	aeadKeysetHandle    *keyset.Handle
	privateKeyFilePath  string
	publicKeyFilePath   string
}

// 创建Crypto实例
func NewCrypto() (*Crypto, error) {

	// 获取密钥文件目录
	home, err := os.UserHomeDir()
	if err != nil {
		return nil, fmt.Errorf("failed to get home directory: %v", err)
	}

	c := &Crypto{
		privateKeyFilePath: filepath.Join(home, ".device_key", "client_private_key.json"),
		publicKeyFilePath:  filepath.Join(home, ".device_key", "client_public_key.json"),
	}

	// 新建或读取密钥文件
	if c.existKeysets() { // 应新建
		// 生成ED25519密钥对
		c.privateKeysetHandle, err = keyset.NewHandle(signature.ED25519KeyTemplate())
		if err != nil {
			return nil, fmt.Errorf("failed to generate private key: %v", err)
		}

		c.publicKeysetHandle, err = c.privateKeysetHandle.Public()
		if err != nil {
			return nil, fmt.Errorf("failed to get public key: %v", err)
		}

		if err := c.saveKeysets(); err != nil {
			return nil, fmt.Errorf("failed to save keysets: %v", err)
		}

		info := c.publicKeysetHandle.KeysetInfo()
		fmt.Printf("公钥信息：%v\n", info)
		info = c.privateKeysetHandle.KeysetInfo()
		fmt.Printf("私钥信息：%v\n", info)
	} else { // 应读取
		aeadKeyFilePath := filepath.Join(home, ".device_key", "aead.json")
		
		// 读取AEAD密钥
		c.aeadKeysetHandle, err = c.readKeysetFromFile(aeadKeyFilePath, nil)
		if err != nil {
			return nil, fmt.Errorf("failed to read AEAD keyset: %v", err)
		}

		// 读取私钥
		c.privateKeysetHandle, err = c.readKeysetFromFile(c.privateKeyFilePath, c.aeadKeysetHandle)
		if err != nil {
			return nil, fmt.Errorf("failed to read private keyset: %v", err)
		}

		// 读取公钥
		c.publicKeysetHandle, err = c.readKeysetFromFile(c.publicKeyFilePath, nil)
		if err != nil {
			return nil, fmt.Errorf("failed to read public keyset: %v", err)
		}
	}

	return c, nil
}

// 获取公钥JSON字符串
func (c *Crypto) GetPublicKeysetString() (string, error) {
	buf := new(strings.Builder)
	writer := keyset.NewJSONWriter(buf)
	if err := insecurecleartextkeyset.Write(c.publicKeysetHandle, writer); err != nil {
		return "", fmt.Errorf("failed to write public keyset: %v", err)
	}
	return buf.String(), nil
}

// 从JSON字符串读取密钥集
func (c *Crypto) ReadKeyset(keyJSON string) (*keyset.Handle, error) {
	reader := keyset.NewJSONReader(strings.NewReader(keyJSON))
	handle, err := insecurecleartextkeyset.Read(reader)
	if err != nil {
		return nil, fmt.Errorf("failed to read keyset: %v", err)
	}
	return handle, nil
}

// 对消息进行签名
func (c *Crypto) Sign(msg, timestamp, nonce string) (string, error) {
	inf := msg + "|" + timestamp + "|" + nonce
	data := []byte(inf)

	// 从Handle获取Signer
	signer, err := signature.NewSigner(c.privateKeysetHandle)
	if err != nil {
		return "", fmt.Errorf("failed to get signer: %v", err)
	}

	signatureBytes, err := signer.Sign(data)
	if err != nil {
		return "", fmt.Errorf("failed to sign: %v", err)
	}

	// 将签名结果编码为Base64
	signatureStr := base64.StdEncoding.EncodeToString(signatureBytes)
	return signatureStr, nil
}

// 保存密钥集
func (c *Crypto) saveKeysets() error {
	home, err := os.UserHomeDir()
	if err != nil {
		return fmt.Errorf("failed to get home directory: %v", err)
	}

	privateKeyFilePath := filepath.Join(home, ".device_key", "client_private_key.json")
	publicKeyFilePath := filepath.Join(home, ".device_key", "client_public_key.json")
	aeadKeyFilePath := filepath.Join(home, ".device_key", "aead.json")

	// 检查私钥文件是否已存在
	if _, err := os.Stat(privateKeyFilePath); err == nil {
		fmt.Println("已有私钥文件，不保存新私钥")
		return nil
	}

	// 创建目录
	parentDir := filepath.Dir(privateKeyFilePath)
	if err := os.MkdirAll(parentDir, 0755); err != nil {
		return fmt.Errorf("failed to create directory: %v", err)
	}

	// 生成AEAD密钥
	c.aeadKeysetHandle, err = keyset.NewHandle(aead.AES128GCMKeyTemplate())
	if err != nil {
		return fmt.Errorf("failed to generate AEAD key: %v", err)
	}

	// 保存AEAD密钥
	aeadFile, err := os.Create(aeadKeyFilePath)
	if err != nil {
		return fmt.Errorf("failed to create AEAD file: %v", err)
	}
	defer aeadFile.Close()

	if err := insecurecleartextkeyset.Write(c.aeadKeysetHandle, keyset.NewJSONWriter(aeadFile)); err != nil {
		return fmt.Errorf("failed to write AEAD keyset: %v", err)
	}
	fmt.Printf("AEAD保存至：%s\n", aeadKeyFilePath)

	// 获取AEAD对象
	a, err := aead.New(c.aeadKeysetHandle)
	if err != nil {
		return fmt.Errorf("failed to get AEAD: %v", err)
	}

	// 保存私钥
	privateFile, err := os.Create(privateKeyFilePath)
	if err != nil {
		return fmt.Errorf("failed to create private key file: %v", err)
	}
	defer privateFile.Close()

	if err := c.privateKeysetHandle.Write(keyset.NewJSONWriter(privateFile), a); err != nil {
		return fmt.Errorf("failed to write private keyset: %v", err)
	}
	fmt.Printf("私钥保存至：%s\n", privateKeyFilePath)

	// 保存公钥
	publicFile, err := os.Create(publicKeyFilePath)
	if err != nil {
		return fmt.Errorf("failed to create public key file: %v", err)
	}
	defer publicFile.Close()

	if err := insecurecleartextkeyset.Write(c.publicKeysetHandle, keyset.NewJSONWriter(publicFile)); err != nil {
		return fmt.Errorf("failed to write public keyset: %v", err)
	}
	fmt.Printf("公钥保存至：%s\n", publicKeyFilePath)

	return nil
}

// 从文件读取密钥集
func (c *Crypto) readKeysetFromFile(keysetFile string, aeadHandle *keyset.Handle) (*keyset.Handle, error) {
	file, err := os.Open(keysetFile)
	if err != nil {
		return nil, fmt.Errorf("failed to open file: %v", err)
	}
	defer file.Close()

	reader := keyset.NewJSONReader(file)

	if aeadHandle == nil {
		// 读取明文密钥
		return insecurecleartextkeyset.Read(reader)
	}

	// 使用AEAD解密读取
	a, err := aead.New(aeadHandle)
	if err != nil {
		return nil, fmt.Errorf("failed to get AEAD: %v", err)
	}

	return keyset.Read(reader, a)
}

// 检查密钥文件是否存在
func (c *Crypto) existKeysets() bool {
	if _, err := os.Stat(c.privateKeyFilePath); err == nil {
		fmt.Println("已有私钥文件，不保存新私钥")
		return false
	}
	return true
}
