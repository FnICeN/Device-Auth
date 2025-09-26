package main

import (
	"GetInfo/util"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"time"
)

type Server struct {
	crypto *util.Crypto
	gdi    *util.GetDeviceInfo
}

// 返回cpuid和公钥
type Response struct {
	CpuID         string `json:"cpuid"`
	PublicKeyJSON string `json:"publicKeyJson"`
}

// 返回签名
type SignResponse struct {
	Sign      string `json:"sign"`
	Timestamp string `json:"timestamp"`
}

// 创建并返回一个新的Server实例，其中包含初始化的Crypto和GetDeviceInfo实例
// Crypto实例可能会创建失败，而GetDeviceInfo实例不会失败
func NewServer() (*Server, error) {
	cryptoInstance, err := util.NewCrypto()
	if err != nil {
		return nil, fmt.Errorf("failed to initialize crypto: %v", err)
	}

	return &Server{
		crypto: cryptoInstance,
		gdi:    util.NewGetDeviceInfo(),
	}, nil
}

func (s *Server) cpuidHandler(w http.ResponseWriter, r *http.Request) {
	// 获取查询参数中的nonce
	nonce := r.URL.RawQuery
	fmt.Println(nonce)

	// 获取CPU ID
	cpuid, err := s.gdi.GetCPUID()
	if err != nil {
		s.handleError(w, fmt.Errorf("failed to get CPU ID: %v", err))
		return
	}

	// 设置CORS头
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")

	if nonce == "" {
		// 没有nonce时，返回cpuid和公钥
		publicKeyJSON, err := s.crypto.GetPublicKeysetString()
		if err != nil {
			s.handleError(w, fmt.Errorf("failed to get public key: %v", err))
			return
		}

		response := Response{
			CpuID:         cpuid,
			PublicKeyJSON: publicKeyJSON,
		}

		if err := json.NewEncoder(w).Encode(response); err != nil {
			s.handleError(w, fmt.Errorf("failed to encode response: %v", err))
			return
		}

		// 打印响应（用于调试）
		respBytes, _ := json.Marshal(response)
		fmt.Println(string(respBytes))
	} else {
		// 有nonce时，返回签名
		timestamp := fmt.Sprintf("%d", time.Now().UnixMilli())
		signed, err := s.crypto.Sign(cpuid, timestamp, nonce)
		if err != nil {
			s.handleError(w, fmt.Errorf("failed to sign: %v", err))
			return
		}

		response := SignResponse{
			Sign:      signed,
			Timestamp: timestamp,
		}

		if err := json.NewEncoder(w).Encode(response); err != nil {
			s.handleError(w, fmt.Errorf("failed to encode response: %v", err))
			return
		}

		// 打印响应（用于调试）
		respBytes, _ := json.Marshal(response)
		fmt.Println(string(respBytes))
	}
}

func (s *Server) handleError(w http.ResponseWriter, err error) {
	log.Printf("Error: %v", err)
	http.Error(w, err.Error(), http.StatusInternalServerError)
}

func main() {
	server, err := NewServer()
	if err != nil {
		log.Fatalf("Failed to create server: %v", err)
	}

	// 设置路由
	http.HandleFunc("/get_cpuid", server.cpuidHandler)

	// 启动服务器
	addr := "127.0.0.1:12345"
	fmt.Printf("服务已启动，监听 %s\n", addr)
	if err := http.ListenAndServe(addr, nil); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}
